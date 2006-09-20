%module example
%{
#include "ftdi.h"
%}

%include<typemaps.i>
%include<arrays_java.i>

%inline %{
#include "ftdi.h"
struct ftdi_context *new_ftdi_context() {
  return (struct ftdi_context*)malloc(sizeof(struct ftdi_context));
}
struct ftdi_eeprom *new_ftdi_eeprom() {
  return (struct ftdi_eeprom*)malloc(sizeof(struct ftdi_eeprom));
}

%}


%exception ftdi_init {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_init() returned nonzero result");
    return $null;
  }
}
int ftdi_init(struct ftdi_context *ftdi);

%exception ftdi_read_data {
  $action
  if (result<0) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_read_data() returned negative result");
    return $null;
  }
}
int ftdi_read_data(struct ftdi_context *ftdi, signed char buf[], int size);

%exception ftdi_write_data {
  $action
  if (result<0) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_write_data() returned negative result");
    return $null;
  }
}
int ftdi_write_data(struct ftdi_context *ftdi, signed char buf[], int size);

%exception ftdi_usb_open {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_usb_open() returned nonzero result");
    return $null;
  }
}
int ftdi_usb_open(struct ftdi_context *ftdi, int vendor, int product);

%exception ftdi_set_baudrate {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdftdi_set_baudrate() returned nonzero result");
    return $null;
  }
}
int ftdi_set_baudrate(struct ftdi_context *ftdi, int baudrate);

%exception ftdi_set_line_property {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_set_line_property() returned nonzero result");
    return $null;
  }
}
int ftdi_set_line_property(struct ftdi_context *ftdi, int bits, int sbit, int parity);

%exception ftdi_set_bitmode {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftftdi_set_bitmodeeturned nonzero result");
    return $null;
  }
}
int ftdi_set_bitmode(struct ftdi_context *ftdi, unsigned char bitmask, unsigned char mode);

%exception ftdi_read_pins {
  $action
  if (result<0) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_read_pins() returned negative result");
    return $null;
  }
}
int ftdi_read_pins(struct ftdi_context *ftdi, signed char pins[]);

%exception ftdi_setflowctrl {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_setflowctrl() returned nonzero result");
    return $null;
  }
}
int ftdi_setflowctrl(struct ftdi_context *ftdi, int flowctrl);

%exception ftdi_usb_reset {
  $action
  if (result) {
    jclass clazz = (*jenv)->FindClass(jenv, "java/io/IOException");
    (*jenv)->ThrowNew(jenv, clazz, "ftdi_usb_reset() nonzero result");
    return $null;
  }
}
int ftdi_usb_reset(struct ftdi_context *ftdi);

/*
int ftdi_set_interface(struct ftdi_context *ftdi, enum ftdi_interface interface);
*/

void ftdi_deinit(struct ftdi_context *ftdi);
void ftdi_set_usbdev (struct ftdi_context *ftdi, usb_dev_handle *usbdev);
    
int ftdi_usb_find_all(struct ftdi_context *ftdi, struct ftdi_device_list **devlist,
                      int vendor, int product);
void ftdi_list_free(struct ftdi_device_list **devlist);
    
int ftdi_usb_open_desc(struct ftdi_context *ftdi, int vendor, int product,
                       const char* description, const char* serial);
int ftdi_usb_open_dev(struct ftdi_context *ftdi, struct usb_device *dev);
    
int ftdi_usb_close(struct ftdi_context *ftdi);
int ftdi_usb_purge_buffers(struct ftdi_context *ftdi);

int ftdi_read_data_set_chunksize(struct ftdi_context *ftdi, unsigned int chunksize);
int ftdi_read_data_get_chunksize(struct ftdi_context *ftdi, unsigned int *chunksize);

int ftdi_write_data_set_chunksize(struct ftdi_context *ftdi, unsigned int chunksize);
int ftdi_write_data_get_chunksize(struct ftdi_context *ftdi, unsigned int *chunksize);

int ftdi_enable_bitbang(struct ftdi_context *ftdi, unsigned char bitmask);
int ftdi_disable_bitbang(struct ftdi_context *ftdi);

int ftdi_set_latency_timer(struct ftdi_context *ftdi, unsigned char latency);
int ftdi_get_latency_timer(struct ftdi_context *ftdi, unsigned char *latency);

// init and build eeprom from ftdi_eeprom structure
void ftdi_eeprom_initdefaults(struct ftdi_eeprom *eeprom);
int  ftdi_eeprom_build(struct ftdi_eeprom *eeprom, signed char output[]);

int ftdi_read_eeprom(struct ftdi_context *ftdi, signed char eeprom[]);
int ftdi_write_eeprom(struct ftdi_context *ftdi, signed char eeprom[]);
int ftdi_erase_eeprom(struct ftdi_context *ftdi);
