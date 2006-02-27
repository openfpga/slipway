// Copyright 2000-2005 the Contributors, as shown in the revision logs.
// Licensed under the Apache Public Source License 2.0 ("the License").
// You may not use this file except in compliance with the License.

package org.ibex.util;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** General <tt>String</tt> and <tt>byte[]</tt> processing functions,
 *  including Base64 and a safe filename transform.
 *
 *  @author adam@ibex.org
 */
public final class Encode {

    public static class QuotedPrintable {
        public static String decode(String s, boolean lax) {
        //
        //   =XX  -> hex representation, must be uppercase
        //   9, 32, 33-60, 62-126 can be literal
        //   9, 32 at end-of-line must get encoded
        //   trailing whitespace must be deleted when decoding
        //   =\n = soft line break
        //   lines cannot be more than 76 chars long
        //

            // lax is used for RFC2047 headers; removes restrictions on which chars you can encode
            return s;
        }
    }


    public static class RFC2047 {
        public static String decode(String s) {
            /*
            try { while (s.indexOf("=?") != -1) {
                String pre = s.substring(0, s.indexOf("=?"));
                s = s.substring(s.indexOf("=?") + 2);

                // MIME charset; FIXME use this
                String charset = s.substring(0, s.indexOf('?')).toLowerCase();
                s = s.substring(s.indexOf('?') + 1);

                String encoding = s.substring(0, s.indexOf('?')).toLowerCase();
                s = s.substring(s.indexOf('?') + 1);

                String encodedText = s.substring(0, s.indexOf("?="));

                if (encoding.equals("b"))      encodedText = new String(Base64.decode(encodedText));

                // except that ANY char can be endoed (unlike real qp)
                else if (encoding.equals("q")) encodedText = MIME.QuotedPrintable.decode(encodedText, true);
                else Log.warn(MIME.class, "unknown RFC2047 encoding \""+encoding+"\"");

                String post = s.substring(s.indexOf("?=") + 2);
                s = pre + encodedText + post;

                // FIXME re-encode when transmitting

            } } catch (Exception e) {
                Log.warn(MIME.class, "error trying to decode RFC2047 encoded-word: \""+s+"\"");
                Log.warn(MIME.class, e);
            }
            */
            return s;
        }
    }


    public static long twoFloatsToLong(float a, float b) {
        return ((Float.floatToIntBits(a) & 0xffffffffL) << 32) | (Float.floatToIntBits(b) & 0xffffffffL); }
    public static float longToFloat1(long l) { return Float.intBitsToFloat((int)((l >> 32) & 0xffffffff)); }
    public static float longToFloat2(long l) { return Float.intBitsToFloat((int)(l & 0xffffffff)); }

    private static final char[] fn =
        new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String toFilename(String s) {
        StringBuffer sb = new StringBuffer();
        try {
            byte[] b = s.getBytes("UTF-8");
            for(int i=0; i<b.length; i++) {
                char c = (char)(b[i] & 0xff);
                if (c == File.separatorChar || c < 32 || c > 126 || c == '%' || (i == 0 && c == '.'))
                    sb.append("%" + fn[(b[i] & 0xf0) >> 8] + fn[b[i] & 0xf]);
                else sb.append(c);
            }
            return sb.toString();
        } catch (UnsupportedEncodingException uee) {
            throw new Error("this should never happen; Java spec mandates UTF-8 support");
        }
    }

    public static String fromFilename(String s) {
        StringBuffer sb = new StringBuffer();
        byte[] b = new byte[s.length() * 2];
        int bytes = 0;
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%') b[bytes++] = (byte)Integer.parseInt(("" + s.charAt(++i) + s.charAt(++i)), 16);
            else b[bytes++] = (byte)c;
        }
        try {
            return new String(b, 0, bytes, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new Error("this should never happen; Java spec mandates UTF-8 support");
        }
    }

    public static class Ascii {
        public static class In extends InputStream {
            public  final int radix;
            private final Reader reader;
            private int blen = 0;
            private byte[] bbuf = new byte[1024 * 16];
            private int clen = 0;
            private char[] cbuf = new char[1024 * 16];
            public In(int radix, InputStream is) {
                // FIXME: radix must be a power of 2
                this.radix = radix;
                this.reader = new InputStreamReader(is);
            }
            public int read() throws IOException {
                byte[] buf = new byte[1];
                while(true) {
                    int numread = read(buf, 0, 1);
                    if (numread<0) return -1;
                    if (numread>0) return (buf[0] & 0xff);
                }
            }
            public long skip(long n) throws IOException {
                while(blen<=0) if (!fillb()) return -1;
                int numskip = Math.min((int)n, blen);
                if (blen > numskip) System.arraycopy(bbuf, numskip, bbuf, 0, blen-numskip);
                blen -= numskip;
                return numskip;
            }                
            public int read(byte[] b) throws IOException { return read(b, 0, b.length); }
            public int available() { return blen; }
            public boolean markSupported() { return false; }
            public void close() throws IOException { reader.close(); }
            public int read(byte[] buf, int off, int len) throws IOException {
                while(blen<=0) if (!fillb()) return -1;
                int numread = Math.min(len, blen);
                System.arraycopy(bbuf, 0, buf, off, numread);
                if (numread < blen) System.arraycopy(bbuf, numread, bbuf, 0, blen-numread);
                blen -= numread;
                return numread;
            }
            public boolean fillc() throws IOException {
                int numread = reader.read(cbuf, clen, cbuf.length - clen);
                if (numread == -1) return false;
                int j = 0;
                for(int i=0; i<numread; i++) {
                    if (!Character.isWhitespace(cbuf[clen+i]))
                        cbuf[clen+(j++)] = cbuf[clen+i];
                }
                clen += j;
                return true;
            }
            public boolean fillb() throws IOException {
                int minChars;
                int bytesPerMinChars;
                switch(radix) {
                    case 2: { minChars = 8; bytesPerMinChars = 1; break; }
                    case 16: { minChars = 2; bytesPerMinChars = 1; break; }
                    default: throw new Error("unsupported");
                }
                while(clen < minChars) if (!fillc()) return false;
                int pos = 0;
                while(pos <= clen - minChars) {
                    bbuf[blen++] = (byte)Integer.parseInt(new String(cbuf, pos, minChars), radix);
                    pos += minChars;
                }
                System.arraycopy(cbuf, pos, cbuf, 0, clen-pos);
                clen -= pos;
                return true;
            }
        }
    }

    private static final byte[] encB64 = {
        (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
        (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
        (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U',
        (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
        (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
        (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
        (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u',
        (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z', (byte)'0', (byte)'1',
        (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7', (byte)'8',
        (byte)'9', (byte)'+', (byte)'/'
    };

    // FIXME could be far more efficient
    public static class Base64InputStream extends ByteArrayInputStream {
        public Base64InputStream(String s) { super(fromBase64(s.getBytes())); }
    }

    public static byte[] toBase64(String data) { return toBase64(data.getBytes()); }

    /** Encode the input data producong a base 64 encoded byte array.
     *  @return A byte array containing the base 64 encoded data. */
    public static byte[] toBase64(byte[] data) {
        byte[]  bytes;
                
        int modulus = data.length % 3;
        if (modulus == 0) {
            bytes = new byte[4 * data.length / 3];
        } else {
            bytes = new byte[4 * ((data.length / 3) + 1)];
        }

        int dataLength = (data.length - modulus);
        int a1, a2, a3;
        for (int i = 0, j = 0; i < dataLength; i += 3, j += 4) {
            a1 = data[i] & 0xff;
            a2 = data[i + 1] & 0xff;
            a3 = data[i + 2] & 0xff;
            
            bytes[j] = encB64[(a1 >>> 2) & 0x3f];
            bytes[j + 1] = encB64[((a1 << 4) | (a2 >>> 4)) & 0x3f];
            bytes[j + 2] = encB64[((a2 << 2) | (a3 >>> 6)) & 0x3f];
            bytes[j + 3] = encB64[a3 & 0x3f];
        }

        int b1, b2, b3;
        int d1, d2;
        switch (modulus) {
                case 0:         /* nothing left to do */
                    break;
                case 1:
                    d1 = data[data.length - 1] & 0xff;
                    b1 = (d1 >>> 2) & 0x3f;
                    b2 = (d1 << 4) & 0x3f;

                    bytes[bytes.length - 4] = encB64[b1];
                    bytes[bytes.length - 3] = encB64[b2];
                    bytes[bytes.length - 2] = (byte)'=';
                    bytes[bytes.length - 1] = (byte)'=';
                    break;
                case 2:
                    d1 = data[data.length - 2] & 0xff;
                    d2 = data[data.length - 1] & 0xff;

                    b1 = (d1 >>> 2) & 0x3f;
                    b2 = ((d1 << 4) | (d2 >>> 4)) & 0x3f;
                    b3 = (d2 << 2) & 0x3f;

                    bytes[bytes.length - 4] = encB64[b1];
                    bytes[bytes.length - 3] = encB64[b2];
                    bytes[bytes.length - 2] = encB64[b3];
                    bytes[bytes.length - 1] = (byte)'=';
                    break;
            }

        return bytes;
    }


    private static final byte[] decB64 = new byte[128];
    static {
        for (int i = 'A'; i <= 'Z'; i++) decB64[i] = (byte)(i - 'A');
        for (int i = 'a'; i <= 'z'; i++) decB64[i] = (byte)(i - 'a' + 26);
        for (int i = '0'; i <= '9'; i++) decB64[i] = (byte)(i - '0' + 52);
        decB64['+'] = 62;
        decB64['/'] = 63;
    }

    /** Decode base 64 encoded input data.
     *  @return A byte array representing the decoded data. */
    public static byte[] fromBase64(byte[] data) {
        byte[]  bytes;
        byte    b1, b2, b3, b4;

        if (data[data.length - 2] == '=') bytes = new byte[(((data.length / 4) - 1) * 3) + 1];
        else if (data[data.length - 1] == '=') bytes = new byte[(((data.length / 4) - 1) * 3) + 2];
        else bytes = new byte[((data.length / 4) * 3)];

        for (int i = 0, j = 0; i < data.length - 4; i += 4, j += 3) {
            b1 = decB64[data[i]];
            b2 = decB64[data[i + 1]];
            b3 = decB64[data[i + 2]];
            b4 = decB64[data[i + 3]];
            
            bytes[j] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[j + 1] = (byte)((b2 << 4) | (b3 >> 2));
            bytes[j + 2] = (byte)((b3 << 6) | b4);
        }

        if (data[data.length - 2] == '=') {
            b1 = decB64[data[data.length - 4]];
            b2 = decB64[data[data.length - 3]];
            bytes[bytes.length - 1] = (byte)((b1 << 2) | (b2 >> 4));
        } else if (data[data.length - 1] == '=') {
            b1 = decB64[data[data.length - 4]];
            b2 = decB64[data[data.length - 3]];
            b3 = decB64[data[data.length - 2]];
            bytes[bytes.length - 2] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 1] = (byte)((b2 << 4) | (b3 >> 2));
        } else {
            b1 = decB64[data[data.length - 4]];
            b2 = decB64[data[data.length - 3]];
            b3 = decB64[data[data.length - 2]];
            b4 = decB64[data[data.length - 1]];
            bytes[bytes.length - 3] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 2] = (byte)((b2 << 4) | (b3 >> 2));
            bytes[bytes.length - 1] = (byte)((b3 << 6) | b4);
        }
        return bytes;
    }

    /** Decode a base 64 encoded String.
     *  @return A byte array representing the decoded data. */
    public static byte[] fromBase64(String data) {
        byte[]  bytes;
        byte    b1, b2, b3, b4;

        if (data.charAt(data.length() - 2) == '=')
            bytes = new byte[(((data.length() / 4) - 1) * 3) + 1];
        else if (data.charAt(data.length() - 1) == '=')
            bytes = new byte[(((data.length() / 4) - 1) * 3) + 2];
        else
            bytes = new byte[((data.length() / 4) * 3)];

        for (int i = 0, j = 0; i < data.length() - 4; i += 4, j += 3) {
            b1 = decB64[data.charAt(i)];
            b2 = decB64[data.charAt(i + 1)];
            b3 = decB64[data.charAt(i + 2)];
            b4 = decB64[data.charAt(i + 3)];
            
            bytes[j] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[j + 1] = (byte)((b2 << 4) | (b3 >> 2));
            bytes[j + 2] = (byte)((b3 << 6) | b4);
        }

        if (data.charAt(data.length() - 2) == '=') {
            b1 = decB64[data.charAt(data.length() - 4)];
            b2 = decB64[data.charAt(data.length() - 3)];
            bytes[bytes.length - 1] = (byte)((b1 << 2) | (b2 >> 4));
        } else if (data.charAt(data.length() - 1) == '=') {
            b1 = decB64[data.charAt(data.length() - 4)];
            b2 = decB64[data.charAt(data.length() - 3)];
            b3 = decB64[data.charAt(data.length() - 2)];
            bytes[bytes.length - 2] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 1] = (byte)((b2 << 4) | (b3 >> 2));
        } else {
            b1 = decB64[data.charAt(data.length() - 4)];
            b2 = decB64[data.charAt(data.length() - 3)];
            b3 = decB64[data.charAt(data.length() - 2)];
            b4 = decB64[data.charAt(data.length() - 1)];
            bytes[bytes.length - 3] = (byte)((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 2] = (byte)((b2 << 4) | (b3 >> 2));
            bytes[bytes.length - 1] = (byte)((b3 << 6) | b4);
        }
        return bytes;
    }


    /** Packs 8-bit bytes into a String of 7-bit chars.
     *  @throws IllegalArgumentException when <tt>len</tt> is not a multiple of 7.
     *  @return A String representing the processed bytes. */
    public static String toStringFrom8bit(byte[] b, int off, int len) throws IllegalArgumentException {
        if (len % 7 != 0) throw new IllegalArgumentException("len must be a multiple of 7");
        StringBuffer ret = new StringBuffer();
        for(int i=off; i<off+len; i += 7) {
            long l = 0;
            for(int j=6; j>=0; j--) {
                l <<= 8;
                l |= (b[i + j] & 0xff);
            }
            for(int j=0; j<8; j++) {
                ret.append((char)(l & 0x7f));
                l >>= 7;
            }
        }
        return ret.toString();
    }

    /** Packs a String of 7-bit chars into 8-bit bytes.
     *  @throws IllegalArgumentException when <tt>s.length()</tt> is not a multiple of 8.
     *  @return A byte array representing the processed String. */
    public static byte[] fromStringTo8bit(String s) throws IllegalArgumentException {
        if (s.length() % 8 != 0) throw new IllegalArgumentException("string length must be a multiple of 8");
        byte[] ret = new byte[(s.length() / 8) * 7];
        for(int i=0; i<s.length(); i += 8) {
            long l = 0;
            for(int j=7; j>=0; j--) {
                l <<= 7;
                l |= (s.charAt(i + j) & 0x7fL);
            }
            for(int j=0; j<7; j++) {
                ret[(i / 8) * 7 + j] = (byte)(l & 0xff);
                l >>= 8;
            }
        }
        return ret;
    }

    public static class JavaSourceCode {

        public static final int LINE_LENGTH = 80 / 4;
        public static void main(String[] s) throws Exception { System.out.println(encode(s[0], s[1], System.in)); }

        public static InputStream decode(String s) throws IOException {
            return new GZIPInputStream(new StringInputStream(s)); }
        
        private static class StringInputStream extends InputStream {
            private final String s;
            private final int length;
            private int pos = 0;
            public StringInputStream(String s) { this.s = s; this.length = s.length(); }
            public int read() {
                byte[] b = new byte[1];
                int numread = read(b, 0, 1);
                if (numread == -1) return -1;
                if (numread == 0) throw new Error();
                return b[0] & 0xff;
            }
            public int read(byte[] b, int off, int len) {
                for(int i=off; i<off+len; i++) {
                    if (pos>=length) return i-off;
                    //int out = s.charAt(pos++);
                    b[i] = (byte)s.charAt(pos++);//(byte)(out > 127 ? 127-out : out);
                }
                return len;
            }
        }

        public static String encode(String packageName, String className, InputStream is) throws IOException {

            // compress first, since the encoded form has more entropy
            ByteArrayOutputStream baos;
            OutputStream os = new GZIPOutputStream(baos = new ByteArrayOutputStream());

            byte[] buf = new byte[1024];
            while(true) {
                int numread = is.read(buf, 0, buf.length);
                if (numread == -1) break;
                os.write(buf, 0, numread);
            }
            os.close();
            buf = baos.toByteArray();
            
            StringBuffer ret = new StringBuffer();
            ret.append("// generated by " + Encode.class.getName() + "\n\n");
            ret.append("package " + packageName + ";\n\n");
            ret.append("public class " + className + " {\n");
            ret.append("    public static final String data = \n");
            for(int pos = 0; pos<buf.length;) {
                ret.append("        \"");
                for(int i=0; i<LINE_LENGTH && pos<buf.length; i++) {
                    String cs = Integer.toOctalString(buf[pos++] & 0xff);
                    while(cs.length() < 3) cs = "0" + cs;
                    ret.append("\\" + cs);
                }
                ret.append("\" +\n");
            }
            ret.append("    \"\";\n");
            ret.append("}\n");
            return ret.toString();
        }

    }

}

