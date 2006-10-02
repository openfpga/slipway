//
// YOU MUST COMPILE THIS WITH -O3 OR THE AVR WILL NOT BE ABLE TO KEEP UP!!!!
//

#define F_CPU 12000000

#if !defined(__AVR_AT94K__)
#error you forgot to put -mmcu=at94k on the command line
#endif

#include <avr/wdt.h>
#include <util/delay.h>
#include <avr/io.h>
#include <avr/interrupt.h>

volatile int32_t upper = 0;

int err = 0;

void initUART0(unsigned int baudRate, unsigned int doubleRate) {
  UBRRHI  = (((baudRate) >> 8) & 0x000F); 
  UBRR0   = ((baudRate) & 0x00FF); 
  UCSR0B |= ((1 << RXEN0) | (1 << TXEN0) | (1 << RXCIE0)); 

  if (doubleRate)
    UCSR0A |=  (1 << U2X0);
  else
    UCSR0A &= ~(1 << U2X0);
}

#define BUFSIZE (1024)

long int numread = 0;
inline void cts(int c) {
  numread++;
  if (c) {
    PORTE &= ~(1 << 7);
  } else {
    PORTE |= (1 << 7);
  }
}


static volatile int sending = 0;
static volatile int32_t interrupt_count = 0;

// RECV //////////////////////////////////////////////////////////////////////////////

char read_buf[BUFSIZE];
volatile int read_buf_head;
volatile int read_buf_tail;
char write_buf[BUFSIZE];
volatile int write_buf_head;
volatile int write_buf_tail;

inline int inc(int x) { x++; if (x>=BUFSIZE) x=0; return x; }
inline int read_full() { return inc(read_buf_tail)==read_buf_head; }
inline int abs(int x) { return x<0 ? -x : x; }
inline int read_size() { return read_buf_tail<read_buf_head ? (read_buf_head-read_buf_tail) : (read_buf_tail-read_buf_head); }
inline int read_empty() { return read_buf_head==read_buf_tail; }
inline int read_nearlyFull() {
  if (read_buf_tail==read_buf_head) return 0;
  if (read_buf_tail < read_buf_head) return (read_buf_head-read_buf_tail) < (BUFSIZE/2);
  return (read_buf_tail-read_buf_head) > (BUFSIZE/2);
}

inline int write_full() { return inc(write_buf_tail)==write_buf_head; }
inline int write_empty() { return write_buf_head==write_buf_tail; }
inline int write_nearlyFull() {
  if (write_buf_tail==write_buf_head) return 0;
  if (write_buf_tail < write_buf_head) return (write_buf_head-write_buf_tail) < (BUFSIZE/2);
  return (write_buf_tail-write_buf_head) > (BUFSIZE/2);
}

int32_t timer = 0;

inline char recv() {
  int q;
  char ret;

  PORTE |=  (1<<3);
  while(read_empty()) cts(1);
  PORTE &= ~(1<<3);

  ret = read_buf[read_buf_head];
  read_buf_head = inc(read_buf_head);
  if (!read_nearlyFull()) cts(1);
  return ret;
}

// Interrupt Handlers //////////////////////////////////////////////////////////////////////////////

ISR(SIG_UART0_DATA) {
  if (write_empty()) {
    UCSR0B &= ~(1 << UDRIE0);
    return;
  }
  char ret = write_buf[write_buf_head];
  write_buf_head = inc(write_buf_head);
  UDR0 = (int)ret;
  sei();
}

void send(char c) {
  PORTE |=  (1<<2);
  while (write_full());
  PORTE &= ~(1<<2);
  write_buf[write_buf_tail] = c;
  write_buf_tail = inc(write_buf_tail);
  UCSR0B |= (1 << UDRIE0);
}


void fpga_interrupts(int on) {
  if (on) {
    //FISUA = 0x1;
    FISCR = 0x80;
    FISUA = 0x01;
  } else {
    FISUA = 0;
    FISCR = 0;
  }
}

inline void conf(int z, int y, int x, int d) {
  FPGAX = x;
  FPGAY = y;
  FPGAZ = z;
  FPGAD = d;
}

#define TIMERVAL 100

ISR(SIG_FPGA_INTERRUPT0) { 
  interrupt_count++;
  sei();
}

volatile int dead = 0;

ISR(SIG_OVERFLOW1) { 
  upper = upper + 1;

  if (!dead) {
    if (PORTE & (1<<5)) PORTE &= ~(1<<5);
    else                PORTE |=  (1<<5);
  }

  TCNT1 = 0;
  sei();
}

//void die() { dead = 1; cli(); PORTE|=(1<<5); _delay_ms(2000); while(1) { } }

void die(int two, int three, int five) {
  dead = 1;
  PORTE &~ ((1<<2) | (1<<3) | (1<<5));
  if (two) PORTE |= (1<<2);
  if (three) PORTE |= (1<<3);
  if (five) PORTE |= (1<<5);
  while(1) { }
}

ISR(SIG_UART0_RECV) {
  if (UCSR0A & (1 << FE0))   die(0, 0, 1);
  if ((UCSR0A & (1 << OR0))) die(1, 1, 1);
  if (read_full()) die(1, 0, 1);

  read_buf[read_buf_tail] = UDR0;
  read_buf_tail = inc(read_buf_tail);
  if (read_nearlyFull()) cts(0);
  SREG |= 0x80;
  sei();
}

inline int hex(char c) {
  if (c >= '0' && c <= '9') return (c - '0');
  if (c >= 'a' && c <= 'f') return ((c - 'a') + 0xa);
  if (c >= 'A' && c <= 'F') return ((c - 'A') + 0xa);
  return -1;
}

int readFPGA() {
  fpga_interrupts(0);
  int ret = FISUA;
  fpga_interrupts(1);
  return ret;
}

int main() {
  DDRE  = (1<<7) | (1<<5) | (1<<3) | (1<<2);
  PORTE = 0;

  PORTE |=  (1<<5);

  read_buf_head = 0;
  read_buf_tail = 0;
  write_buf_head = 0;
  write_buf_tail = 0;
  initUART0(1, 0);  //for slow board

  EIMF = 0xFF;
  SREG = INT0;
  sei();

  TCNT1 = 0;
  TIFR&=~(1<<TOV1);
  TIMSK|=(1<<TOIE1);
  TCCR1B = 3;

  cts(0);
  cts(1);

  int x=0, y=0, z=0;
  int flag=0;
  for(;;) {
    int i, d=0;
    int r = recv();
    switch(r) {
      case 0:
        send('O');
        send('B');
        send('I');
        send('T');
        send('S');
        fpga_interrupts(1);
        if (flag) die(0, 1, 1);
        break;

      case 1:
        z = recv();
        y = recv();
        x = recv();
        d = recv();
        conf(z, y, x, d);
        break;

      case 2:
        flag=1;
        send(readFPGA());
        break;

      case 3: {
        int32_t local_interrupt_count = interrupt_count;
        interrupt_count = 0;
        send((local_interrupt_count >> 24) & 0xff);
        send((local_interrupt_count >> 16) & 0xff);
        send((local_interrupt_count >>  8) & 0xff);
        send((local_interrupt_count >>  0) & 0xff);
 
        int32_t local_timer = TCNT1;
        int32_t local_upper = upper;
        TCCR1B = 0;
        TIFR&=~(1<<TOV1);
        TIMSK|=(1<<TOIE1);
        upper = 0;
        TCNT1 = 0;
        TCCR1B = 3;
        send((local_upper >>  8) & 0xff);
        send((local_upper >>  0) & 0xff);
        send((local_timer >>  8) & 0xff);
        send((local_timer >>  0) & 0xff);
        break;
      }

        /*
      case 3:
        //init_timer();
        break;
      case 4:
        sending = 1;
        break;
      case 5:
        sending = 0;
        break;
        */
    }
  }
  return 0;

}  

