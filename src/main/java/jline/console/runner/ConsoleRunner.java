/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console.runner;
 
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
 
/**
 * A pass-through application that sets the system input stream to a
 * {@link ConsoleReader} and invokes the specified main method.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @since 2.6
 */
public class ConsoleRunner
{
    public static final String property = "jline.history";

    // FIXME: This is really ugly... re-write this

    public static void main(final String[] args) throws Exception {
        List<String> argList = Arrays.asList(args);
        if (argList.size() == 0) {
            usage();
            return;
        }
 
        String historyFileName = System.getProperty(ConsoleRunner.property, null);
 
        String mainClass = argList.remove(0);
        ConsoleReader reader = new ConsoleReader();
 
        if (historyFileName != null) {
            reader.setHistory(new FileHistory(new File(System.getProperty("user.home"),
                String.format(".jline-%s.%s.history", mainClass, historyFileName))));
        }
        else {
            reader.setHistory(new FileHistory(new File(System.getProperty("user.home"),
                String.format(".jline-%s.history", mainClass))));
        }
 
        String completors = System.getProperty(ConsoleRunner.class.getName() + ".completers", "");
        List<Completer> completorList = new ArrayList<Completer>();
 
        for (StringTokenizer tok = new StringTokenizer(completors, ","); tok.hasMoreTokens();) {
            Object obj = Class.forName(tok.nextToken()).newInstance();
            completorList.add((Completer) obj);
        }
 
        if (completorList.size() > 0) {
            reader.addCompleter(new ArgumentCompleter(completorList));
        }
 
        ConsoleReaderInputStream.setIn(reader);
 
        try {
            Class type = Class.forName(mainClass);
            Method method = type.getMethod("main", new Class[]{String[].class});
            method.invoke(null);
        }
        finally {
            // just in case this main method is called from another program
            ConsoleReaderInputStream.restoreIn();
        }
    }
 
    private static void usage() {
        System.out.println("Usage: \n   java " + "[-Djline.history='name'] "
            + ConsoleRunner.class.getName()
            + " <target class name> [args]"
            + "\n\nThe -Djline.history option will avoid history"
            + "\nmangling when running ConsoleRunner on the same application."
            + "\n\nargs will be passed directly to the target class name.");
    }
}