
led0: obits.jar
	java -Djava.library.path=/usr/lib \
	     -cp /usr/share/java/RXTXcomm.jar:obits.jar \
	     edu.berkeley.obits.AtmelSerial < bitstreams/led0.md4

led1: obits.jar
	java -Djava.library.path=/usr/lib \
	     -cp /usr/share/java/RXTXcomm.jar:obits.jar \
	     edu.berkeley.obits.AtmelSerial < bitstreams/led1.md4

obits.jar: $(shell find src -name \*.java)
	javac -cp /usr/share/java/RXTXcomm.jar -d build $`
	cd build; jar cvf ../$@ .

# -O3 is required; otherwise the poor AVR can't keep up with us!
avrdrone.hex:
	avr-gcc -O3 -mmcu=at94k src/edu/berkeley/obits/device/atmel/AvrDrone.c
	avr-objcopy -O ihex a.out $@


