//
// YOU MUST COMPILE THIS WITH -O3 OR THE AVR WILL NOT BE ABLE TO KEEP UP!!!!
//

#define F_CPU 3960000

#if !defined(__AVR_AT94K__)
#error you forgot to put -mmcu=at94k on the command line
#endif

#include <avr/wdt.h>
#include <util/delay.h>
#include <avr/io.h>
#include <avr/interrupt.h>

unsigned char val = 0;

volatile unsigned char dataReady = 0;
volatile unsigned char data = 0;

void initUART1(unsigned int baudRate, unsigned int doubleRate) {
  UBRRHI = (((baudRate) >> 8) & 0x000F); 
  UBRR1  = ((baudRate) & 0x00FF); 
  UCSR1B |= ((1 << RXEN1) | (1 << TXEN1) | (1 << RXCIE1)); 
  /*
  if (doubleRate)
    UCSR1A |= (1 << U2X1);
  else
    UCSR1A &= ~(1 << U2X1);
  */
}

#define BUFSIZE (1024 *2)
static char buf[BUFSIZE];
volatile static int head = 0;
volatile static int tail = 0;

void send(char c) {
  while(!(UCSR1A & (1 << UDRE1)));           /* Wait for data Regiester to be empty */
  UDR1 = (int)c;
}

inline void portd(int bit, int on) {
  if (on) {
    PORTD &= ~(1<<bit);
  } else {
    PORTD |= (1<<bit);
  }
}
inline void cts(int c) {
  if (c) {
    PORTE &= ~(1 << 4);
    portd(0, 1);
  } else {
    PORTE |= (1 << 4);
    portd(0, 0);
  }
}

inline int inc(int x) { x++; if (x>=BUFSIZE) x=0; return x; }
inline int full() { return inc(tail)==head; }
inline int nearlyFull() {
  if (tail==head) return 0;
  if (tail < head) return (head-tail) < (BUFSIZE/2);
  return (tail-head) > (BUFSIZE/2);
}
inline int empty() { return head==tail; }

inline char recv() {
  int q;
  char ret;
  while(empty()) cts(1);
  ret = buf[head];
  head = inc(head);
  if (!nearlyFull()) cts(0);
  return ret;
}

void init() {
  EIMF  = 0xFF;                          /* Enalbe External Interrrupt*/  
  DDRD = 0xFF;                           /* Configure PORTD as Output */
  DDRE = 1 << 4;                         /* ability to write to E */
  initUART1(1, 0);
  SREG |= 0x80;
  sei();
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
ISR(SIG_OVERFLOW0) { 
  PORTD = ~FISUA;
  TCNT0 = TIMERVAL;           // load the nearest-to-one-second value  into the timer0
  TIMSK |= (1<<TOIE0);        //enable the compare match1 interrupt and the timer/counter0 overflow interrupt
  sei();
} 
void init_timer()  { 
  TCCR0 |= (1<<CS00);         // set the timer0 prescaler to CK
  TCNT0 = TIMERVAL;           // load the nearest-to-one-second value  into the timer0
  TIMSK |= (1<<TOIE0);        //enable the compare match1 interrupt and the timer/counter0 overflow interrupt
} 

ISR(SIG_INTERRUPT1) {   // use interrupt1 since interrupt0 is sent by the watchdog (I think)
  doreset();
}

void die() { cli(); cts(0); _delay_ms(2000); while(1) { } }
ISR(SIG_UART1_RECV) {
  if (UCSR1A & (1 << FE1))   { portd(2,0); portd(3,1); die(); }  // framing error, lock up with LED=01
  if ((UCSR1A & (1 << OR1))) { portd(2,1); portd(3,0); die(); }  // overflow; lock up with LED=10
  if (full())                { portd(2,1); portd(3,1); die(); }  // buffer overrun
  buf[tail] = UDR1;
  tail = inc(tail);
  SREG |= 0x80;
  sei();
  if (nearlyFull()) cts(0);
}

inline int hex(char c) {
  if (c >= '0' && c <= '9') return (c - '0');
  if (c >= 'a' && c <= 'f') return ((c - 'a') + 0xa);
  if (c >= 'A' && c <= 'F') return ((c - 'A') + 0xa);
  return -1;
}

int main() {
  int count;
  init();
  cts(0);
  send('O');
  send('B');
  send('I');
  send('T');
  send('S');
  send('\n');
  cts(1);
  for(;;) {
    int i, x=0, y=0, z=0, d=0;
    switch(recv()) {
      case 1:
        z = recv();
        y = recv();
        x = recv();
        d = recv();
        portd(1,1);
        conf(z, y, x, d);
        portd(1,0);
        break;
      case 2:
        portd(1,1);
        send(FISUA);
        portd(1,0);
        break;
      case 3:
        init_timer();
        break;
      default: die();
    }
  }
  return 0;
}  

