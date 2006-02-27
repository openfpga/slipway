// Copyright 2000-2005 the Contributors, as shown in the revision logs.
// Licensed under the Apache Public Source License 2.0 ("the License").
// You may not use this file except in compliance with the License.

package org.ibex.util;

import java.util.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;

// FEATURE: logging exceptions should automatically unwrap exceptions

/** Easy to use logger.
 * 
 * @author adam@ibex.org
 */
public class Log {
    private static final SimpleDateFormat formatDate = new SimpleDateFormat("EEE dd MMM yyyy");
    private static final SimpleDateFormat formatTime = new SimpleDateFormat("[EEE HH:mm:ss] ");
    private static final Hashtable threadAnnotations = new Hashtable();

    public static boolean on            = System.getProperty("ibex.log.on", "true").equals("true");
    public static boolean color         = System.getProperty("ibex.log.color", "true").equals("true");
    public static boolean verbose       = System.getProperty("ibex.log.verbose", "false").equals("true");
    public static boolean logDates      = System.getProperty("ibex.log.dates", "false").equals("true");
    public static boolean notes         = System.getProperty("ibex.log.notes.on", "true").equals("true");
    public static boolean stackTraces   = System.getProperty("ibex.log.stackTraces", "true").equals("true");
    public static int maximumNoteLength = Integer.parseInt(System.getProperty("ibex.log.notes.maximumLength", (1024 * 32)+""));
    public static boolean rpc           = false;
    public static int lastDay = -1;

    public static PrintStream logstream = System.err;

    public static void flush() { logstream.flush(); }
    public static void email(String address) { throw new Error("FIXME not supported"); }
    public static void file(String filename) throws IOException {
        // FIXME security
        logstream = new PrintStream(new FileOutputStream(filename));
    }
    public static void tcp(String host, int port) throws IOException {
        // FIXME security
        logstream = new PrintStream(new Socket(InetAddress.getByName(host), port).getOutputStream());
    }

    public static void setThreadAnnotation(String s) { threadAnnotations.put(Thread.currentThread(), s); }

    /** 
     *  Notes can be used to attach log messages to the current thread
     *  if you're not sure you want them in the log just yet.
     *  Originally designed for retroactively logging socket-level
     *  conversations only if an error is encountered
     */
    public static void note(String s) {
        if (!notes) return;
        StringBuffer notebuf = notebuf();
        notebuf.append(s);
        if (notebuf.length() > maximumNoteLength) {
            notebuf.reverse();
            notebuf.setLength(maximumNoteLength * 3 / 4);
            notebuf.reverse();
        }
    }
    public static void clearnotes() { if (!notes) return; notebuf().setLength(0); }

    private static final Basket.Map notebufs = new Basket.Hash();
    public static StringBuffer notebuf() {
        StringBuffer ret = (StringBuffer)notebufs.get(Thread.currentThread());
        if (ret == null) {
            ret = new StringBuffer(16 * 1024);
            notebufs.put(Thread.currentThread(), ret);
        }
        return ret;
    }

    /** true iff nothing has yet been logged */
    public static boolean firstMessage = true;

    /** message can be a String or a Throwable */
    public static synchronized void echo(Object o, Object message) { log(o, message, ECHO); }
    public static synchronized void diag(Object o, Object message) { log(o, message, DIAGNOSTIC); }
    public static synchronized void debug(Object o, Object message) { log(o, message, DEBUG); }
    public static synchronized void info(Object o, Object message) { log(o, message, INFO); }
    public static synchronized void warn(Object o, Object message) { log(o, message, WARN); }
    public static synchronized void error(Object o, Object message) { log(o, message, ERROR); }

    // these two logging levels serve ONLY to change the color; semantically they are the same as DEBUG
    private static final int DIAGNOSTIC = -2;
    private static final int ECHO = -1;

    // the usual log4j levels, minus FAIL (we just throw an Error in that case)
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static final int SILENT = Integer.MAX_VALUE;
    public static int level = INFO;

    private static final int BLUE = 34;
    private static final int GREEN = 32;
    private static final int CYAN = 36;
    private static final int RED = 31;
    private static final int PURPLE = 35;
    private static final int BROWN = 33;
    private static final int GRAY = 37;
    
    private static String colorize(int color, boolean bright, String s) {
        if (!Log.color) return s;
        return
            "\033[40;" + (bright?"1;":"") + color + "m" +
            s +
            "\033[0m";
    }

    private static String lastClassName = null;
    private static synchronized void log(Object o, Object message, int level) {
        if (level < Log.level) return;
        if (firstMessage && !logDates) {
            firstMessage = false;
            logstream.println(colorize(GREEN, false, "==========================================================================="));

            // FIXME later: causes problems with method pruning
            //diag(Log.class, "Logging enabled at " + new java.util.Date());

            if (color) diag(Log.class, "logging messages in " +
                colorize(BLUE, true, "c") +
                colorize(RED, true, "o") +
                colorize(CYAN, true, "l") +
                colorize(GREEN, true, "o") +
                colorize(PURPLE, true, "r"));
        }

        String classname;
        if (o instanceof Class) {
            classname = ((Class)o).getName();
            if (classname.indexOf('.') != -1) classname = classname.substring(classname.lastIndexOf('.') + 1);
        }
        else if (o instanceof String) classname = (String)o;
        else classname = o.getClass().getName();

        if (classname.equals(lastClassName)) classname = "";
        else lastClassName = classname;
        
        if (classname.length() > (logDates ? 14 : 20)) classname = classname.substring(0, (logDates ? 14 : 20));
        while (classname.length() < (logDates ? 14 : 20)) classname = " " + classname;
        classname = classname + (classname.trim().length() == 0 ? "  " : ": ");
        classname = colorize(GRAY, true, classname);
        classname = classname.replace('$', '.');

        if (logDates) {
            Calendar cal = Calendar.getInstance();
            if (lastDay < 0 || lastDay != cal.get(Calendar.DAY_OF_YEAR)) {
                lastDay = cal.get(Calendar.DAY_OF_YEAR);
                String now = formatDate.format(cal.getTime());
                logstream.println();
                logstream.println(colorize(GREEN, false, "=== " + now + " =========================================================="));
            }
            classname = formatTime.format(cal.getTime()) + classname;
        }

        String annot = (String)threadAnnotations.get(Thread.currentThread());
        if (annot != null) classname += annot;

        if (message instanceof Throwable) {
            if (level < ERROR) level = WARN;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((Throwable)message).printStackTrace(new PrintStream(baos));
            if (notes && notebuf().length() > 0) {
                PrintWriter pw = new PrintWriter(baos);
                pw.println();
                pw.println("Thread notes:");
                pw.println(notebuf().toString());
                clearnotes();
                pw.flush();
            }
            byte[] b = baos.toByteArray();
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b)));
            try {
                if (stackTraces) {
                    String s = null;
                    String m = "";
                    while((s = br.readLine()) != null) m += s + "\n";
                    if (m.length() > 0) log(o, m.substring(0, m.length() - 1), level);
                } else {
                    String m = br.readLine();
                    int ok = 0;
                    do {
                        String s = br.readLine();
                        if (s == null) break;
                        if (s.indexOf('(') != -1) {
                            String shortened = s.substring(s.indexOf('(')+1);
                            shortened = shortened.substring(0, shortened.indexOf(')'));
                            m += " " + shortened;
                            if (ok > 1) m = m.substring(0, Math.min(m.length(), 78));
                            ok++;
                        }
                    } while (m.length() < 78);
                    log(o, m, level);
                }
                lastClassName = "";
            } catch (IOException e) {
                // FEATURE: use org.ibex.io.Stream's here
                logstream.println(colorize(RED, true, "Logger: exception thrown by ByteArrayInputStream;" +
                                           " this should not happen"));
            }
            return;
        }

        String str = message.toString();
        if (str.indexOf('\n') != -1) lastClassName = "";
        while(str.indexOf('\t') != -1)
            str = str.substring(0, str.indexOf('\t')) + "    " + str.substring(str.indexOf('\t') + 1);

        classname = colorize(GRAY, false, classname);
        int levelcolor = GRAY;
        boolean bright = true;
        switch (level) {
            case DIAGNOSTIC:  levelcolor = GREEN; bright = false; break;
            case ECHO:        levelcolor = BLUE;  bright = true;  break;
            case DEBUG:       levelcolor = BROWN; bright = true;  break;
            case INFO:        levelcolor = GRAY;  bright = false; break;
            case WARN:        levelcolor = BROWN; bright = false; break;
            case ERROR:       levelcolor = RED;   bright = true;  break;
        }

        while(str.indexOf('\n') != -1) {
            logstream.println(classname + colorize(levelcolor, bright, str.substring(0, str.indexOf('\n'))));
            classname = logDates ? "                " : "                      ";
            classname = colorize(GRAY,false,classname);
            str = str.substring(str.indexOf('\n') + 1);
        }
        logstream.println(classname + colorize(levelcolor, bright, str));
    }

}
