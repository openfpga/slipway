ifeq ($(shell uname),Darwin)
linkerflags = -Wl,-framework -Wl,IOKit -Wl,-framework -Wl,CoreFoundation
jnilib      = libFtdiUartNative.jnilib
else
linkerflags =
jnilib      = libFtdiUartNative.so
endif

## slipway ############################################################################

run: slipway.jar 
	java -cp slipway.jar edu.berkeley.slipway.Demo 30

build/src/com/ftdi/usb/FtdiUart.c: src/com/ftdi/usb/FtdiUart.i
	mkdir -p `dirname $@`
	mkdir -p src/com/ftdi/usb
	swig -module FtdiUartNative -noproxy -package com.ftdi.usb -o $@ -outdir `dirname $@` -java $<

build/$(jnilib): build/src/com/ftdi/usb/FtdiUart.c upstream/libusb/.built
	gcc -I. -Iupstream/libftdi -Iupstream/usb \
		-I$(JAVA_HOME)/include \
		$< \
		upstream/libftdi/ftdi.c \
		upstream/libusb/.libs/libusb.a \
		$(linkerflags) \
		-o $@ -dynamiclib -framework JavaVM

slipway.jar: build/$(jnilib) $(shell find src build/src -name \*.java) bitstreams/slipway_drone.bst
	mkdir -p build
	javac -d build $(shell find src build/src -name \*.java)
	cp bitstreams/slipway_drone.bst build/edu/berkeley/slipway/
	cd build; jar cvf ../$@ .



## libusb ##############################################################################

upstream/libusb:
	mkdir -p `dirname $@`
	cd upstream; curl http://umn.dl.sourceforge.net/sourceforge/libusb/libusb-0.1.12.tar.gz | tar -xvzf -
	cd upstream; mv libusb-0.1.12 libusb

upstream/libusb/.built: upstream/libusb
	cd upstream/libusb; \
		./configure && \
		make
	touch $@

#java = java -Djava.library.path=$(shell pwd)/lib/ -cp lib/RXTXcomm.jar:slipway.jar



## for rebuilding usbdrone.hex ###########################################################

build/slipway_drone.hex: src/edu/berkeley/slipway/FtdiBoardSlave.c  upstream/avr-libc/.built
	upstream/prefix/bin/avr-gcc -O3 -mmcu=at94k $< -o $@.o
	upstream/prefix/bin/avr-objcopy -O ihex $@.o $@

# this only works on my personal setup [adam]
bitstreams/slipway_drone.bst: build/slipway_drone.hex
	cp $< /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	fs flush /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	echo okay...
	read
	rm /afs/research.cs.berkeley.edu/user/megacz/edu.berkeley.obits/usbdrone.hex
	diff -u /afs/research.cs.berkeley.edu/user/megacz/stupid/fpslic_stupid.bst $@ && exit -1; true
	mv /afs/research.cs.berkeley.edu/user/megacz/stupid/fpslic_stupid.bst $@
	touch $@



## avr-gcc toolchain and libc ################################################################

upstream/binutils:
	cd upstream; curl http://ftp.gnu.org/pub/pub/pub/gnu/binutils/binutils-2.16.1.tar.bz2 | tar -xvjf -
	cd upstream; mv binutils-2.16.1 binutils

upstream/binutils/.built: upstream/binutils
	mkdir -p upstream/prefix
	cd upstream/binutils; \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin \
		./configure --prefix=$(shell pwd)/upstream/prefix --target=avr && \
		make && \
		make install
	touch $@

upstream/gcc:
	cd upstream; curl http://ftp.gnu.org/pub/gnu/gcc/gcc-4.0.3/gcc-core-4.0.3.tar.bz2 | tar -xvjf -
	cd upstream; mv gcc-4.0.3 gcc

upstream/gcc/.built: upstream/gcc upstream/binutils/.built
	mkdir -p upstream/prefix
	mkdir -p upstream/gcc-build
	cd upstream/gcc-build; \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin \
		../gcc/configure --prefix=$(shell pwd)/upstream/prefix --target=avr && \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin make && \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin make install
	touch $@

upstream/avr-libc:
	cd upstream; curl http://download.savannah.gnu.org/releases/avr-libc/avr-libc-1.4.3.tar.bz2 | tar -xvjf -
	cd upstream; mv avr-libc-1.4.3 avr-libc

upstream/avr-libc/.built: upstream/avr-libc upstream/gcc/.built
	cd upstream/avr-libc; \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin \
		./configure --prefix=$(shell pwd)/upstream/prefix --host=avr && \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin make && \
		PATH=$$PATH:$(shell pwd)/upstream/prefix/bin make install
	touch $@