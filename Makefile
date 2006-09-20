
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

demo: ftdi.jar
	java -cp ftdi.jar edu.berkeley.obits.device.atmel.Demo

build/src/com/ftdi/usb/FtdiUart.c: src/com/ftdi/usb/FtdiUart.i
	mkdir -p `dirname $@`
	mkdir -p src/com/ftdi/usb
	swig -noproxy -package com.ftdi.usb -outdir `dirname $@` -java $<

build/libFtdi.jnilib: build/src/com/ftdi/usb/FtdiUart.c
	gcc -I. -I/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Headers/ \
		$< -o $@ -lftdi -dynamiclib -framework JavaVM

javafiles := $(shell find src build/src -name \*.java)

ftdi.jar: $(javafiles) build/libFtdi.jnilib
	mkdir -p build
	javac -d build $(javafiles)
	cd build; jar cvf ../$@ .

drone.hex: src/drone.c
	avr-gcc -mmcu=at94k -O3 $<
	avr-objcopy -O ihex a.out $@

rcompile:
	cp src/edu/berkeley/obits/device/atmel/usbdrone.c /afs/megacz.com/goliath/work/tmp/
	ssh research.cs.berkeley.edu 'cd /afs/megacz.com/goliath/work/tmp; avr-gcc -O3 -mmcu=at94k usbdrone.c; avr-objcopy -O ihex a.out usbdrone.hex'
	cp /afs/megacz.com/goliath/work/tmp/usbdrone.hex /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	fs flush /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	echo okay...
	read
	rm /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	diff -u /afs/research.cs.berkeley.edu/user/megacz/stupid/fpslic_stupid.bst bitstreams/usbdrone.bst && exit -1; true
	mv /afs/research.cs.berkeley.edu/user/megacz/stupid/fpslic_stupid.bst bitstreams/usbdrone.bst
	make demo