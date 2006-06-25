
java = java -Djava.library.path=$(shell pwd)/lib/ -cp lib/RXTXcomm.jar:obits.jar

led0: obits.jar 
	$(java) edu.berkeley.obits.AtmelSerial < bitstreams/led0.md4

led1: obits.jar
	$(java) edu.berkeley.obits.AtmelSerial < bitstreams/led1.md4

run: obits.jar
	$(java) edu.berkeley.obits.AtmelSerial < stupid.md4

obits.jar: $(shell find src -name \*.java)
	javac -cp lib/RXTXcomm.jar -d build $(shell find src -name \*.java)
	cd build; jar cvf ../$@ .

# -O3 is required; otherwise the poor AVR can't keep up with us!
avrdrone.hex: src/edu/berkeley/obits/device/atmel/AvrDrone.c
	avr-gcc -O3 -mmcu=at94k $<
	avr-objcopy -O ihex a.out $@


