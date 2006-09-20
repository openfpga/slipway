//
// YOU MUST COMPILE THIS WITH -O3 OR THE AVR WILL NOT BE ABLE TO KEEP UP!!!!
//

//#define F_CPU 3960000
#define F_CPU 12000000

#if !defined(__AVR_AT94K__)
#error you forgot to put -mmcu=at94k on the command line
#endif

#include <avr/wdt.h>
#include <util/delay.h>
#include <avr/io.h>
#include <avr/interrupt.h>

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

inline void portd(int bit, int on) {
  /*
  if (on) {
    PORTD &= ~(1<<bit);
  } else {
    PORTD |= (1<<bit);
  }
  */
}

long int numread = 0;
inline void cts(int c) {
  numread++;
  if (c /*&& numread < 10000*/) {
    PORTE |= (1 << 4);
    PORTE |= (1 << 2);
    portd(0, 0);
  } else {
    PORTE &= ~(1 << 4);
    PORTE &= ~(1 << 2);
    portd(0, 1);
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

inline char recv() {
  int q;
  char ret;
  while(read_empty()) cts(1);
  ret = read_buf[read_buf_head];
  read_buf_head = inc(read_buf_head);
  if (!read_nearlyFull()) cts(1);
  return ret;
}

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
  while (write_full());
  write_buf[write_buf_tail] = c;
  write_buf_tail = inc(write_buf_tail);
  if (PORTE & (1<<2)) PORTE &= ~(1<<2);
  else                PORTE |=  (1<<2);
  UCSR0B |= (1 << UDRIE0);
  if (PORTE & (1<<3)) PORTE &= ~(1<<3);
  else                PORTE |=  (1<<3);
}


void fpga_interrupts(int on) {
  if (on/* && interrupt_count<301*/) {
    //FISUA = 0x1;
    FISCR = 0x80;
    FISUD = 0x08;
  } else {
    FISUD = 0;
    FISCR = 0;
  }
}

void init() {
  read_buf_head = 0;
  read_buf_tail = 0;
  write_buf_head = 0;
  write_buf_tail = 0;
  //initUART0(1, 0);  //for slow board
  initUART0(0, 0);  //for slow board
  //initUART0(0, 1);  //for slow board
}

void conf(int z, int y, int x, int d) {
  FPGAX = x;
  FPGAY = y;
  FPGAZ = z;
  FPGAD = d;
}

void doreset() {
  int i;
  for(i=0; i<5; i++) {
    PORTD = ~0x01;
    _delay_ms(50);
    PORTD = ~0x02;
    _delay_ms(50);
    PORTD = ~0x04;
    _delay_ms(50);
    PORTD = ~0x08;
    _delay_ms(50);
  }
  PORTD = ~0x00;
  wdt_enable(WDTO_250MS);
  while(1) { }
}

#define TIMERVAL 100
int portdc = 0;

ISR(SIG_FPGA_INTERRUPT15) { 
  PORTD = portdc++;
  interrupt_count++;
  //PORTD = ~(interrupt_count & 0xff);
  //if (interrupt_count >= 301) fpga_interrupts(0);
  //sei();
  fpga_interrupts(1);
}
/*
ISR(SIG_OVERFLOW0) { 
  fpga_interrupts(0);
  PORTD = ~FISUA;
  TCNT0 = TIMERVAL;           // load the nearest-to-one-second value  into the timer0
  TIMSK |= (1<<TOIE0);        // enable the compare match1 interrupt and the timer/counter0 overflow interrupt
  if (sending) UDR1 = FISUA;
  fpga_interrupts(1);
  sei();
} 
void init_timer()  { 
  TCCR0 |= (1<<CS00);         // set the timer0 prescaler to CK
  TCNT0 = TIMERVAL;           // load the nearest-to-one-second value  into the timer0
  TIMSK |= (1<<TOIE0);        //enable the compare match1 interrupt and the timer/counter0 overflow interrupt
} 
*/
ISR(SIG_INTERRUPT0) {   // use interrupt1 since interrupt0 is sent by the watchdog (I think)
  SREG = 0;
  //PORTE |=  (1<<0);
  sei();
}
void die() { cli(); cts(0); _delay_ms(2000); while(1) { portd(2,0); portd(2,1); } }

ISR(SIG_UART0_RECV) {

  if (UCSR0A & (1 << FE0))    err = 201;//{ portd(2,0); portd(3,1); die(); }  // framing error, lock up with LED=01
  if ((UCSR0A & (1 << OR0)))  err = 202;//{ portd(2,1); portd(3,0); die(); }  // overflow; lock up with LED=10
  if (read_full())            err = 203;//{ portd(2,1); portd(3,1); die(); }  // buffer overrun

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

int main() {
  DDRE = (1<<7) | (1<<5) | (1<<4) | (1<<3) | (1<<2);
  PORTE = 0;

  init();

  EIMF = 0xFF;
  SREG = INT0;
  sei();

  PORTE &= ~(1<<7);

  int count = 0;
  long long bad = 0;
  int left = 0;
  char v = 0;
  long int oldi = 0;
  cts(0);
  cts(1);
  /*
  while(1) {
    long int i = recv() & 0xff;
    //if (i < 0) err = 200;
    //if (err < 200) {
      long int newi = oldi+1;
      if (newi >= 256) newi -= 256;
      if (i != newi) err++;
      //}
    oldi = i;

    send(err >= 10 ? 255 : i);
    //send(err);
  }
  */

  PORTE |=  (1<<3);
  PORTE |=  (1<<5);
  recv();
  send('O');
  send('B');
  send('I');
  send('T');
  send('S');
  send('\n');

  int x=0, y=0, z=0;
  //while(1) send(/*FISUA*/2);
  for(;;) {
    /*
    if (PORTE & (1<<6)) PORTE &= ~(1<<6);
    else                PORTE |=  (1<<6);
    */
    int i, d=0;
    int r = recv();
    switch(r) {
      case 1:
        z = recv();
        y = recv();
        x = recv();
        d = recv();
        //portd(1,1);
        conf(z, y, x, d);
        //portd(1,0);
        break;
      case 2:
        //fpga_interrupts(0);
        //portd(1,1);
        send(FISUA);
        //portd(1,0);
        //fpga_interrupts(1);
        break;
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
      case 6: {
        int32_t local_interrupt_count = interrupt_count;
        interrupt_count = 0;
        send((local_interrupt_count >> 24) & 0xff);
        send((local_interrupt_count >> 16) & 0xff);
        send((local_interrupt_count >>  8) & 0xff);
        send((local_interrupt_count >>  0) & 0xff);
        fpga_interrupts(1);
        break;
      }
        */
        /*
      default: {
        if ((r & 0x80) == 0x80) {
          switch (r & 0x44) {
            case 0x44: z = recv(); break;
            case 0x40: z++;        break;
            case 0x04: z--;        break;
          }
          switch (r & 0x22) {
            case 0x22: y = recv(); break;
            case 0x20: y++;        break;
            case 0x02: y--;        break;
          }
          switch (r & 0x11) {
            case 0x11: x = recv(); break;
            case 0x10: x++;        break;
            case 0x01: x--;        break;
          }
          d = recv();
          portd(1,1);
          conf(z, y, x, d);
          portd(1,0);
          break;
        }
        die();
      }
        */
    }
  }
  return 0;

}  

