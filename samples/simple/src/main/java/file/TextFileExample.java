/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package file;

import java.io.File;

import junit.framework.TestResult;
import junit.framework.TestSuite;
import util.SimpleRunner;

public class TextFileExample extends SimpleRunner
 {
    public TextFileExample() {
        super("file/TextFileExample.xml");
    }

    public static void main(String[] args) {
        TestSuite suite = new TestSuite(TextFileExample.class);
        TestResult result = new TestResult();
        suite.run(result);
    }

    protected void runSample() throws Exception {

        File f = new File("./out/test");
        assertTrue("file does not exist: " + f.getAbsolutePath(), f.exists());
        assertTrue("Could not move file from out to in", f.renameTo(new File("./in/test")));

        Thread.sleep(1000);
        assertTrue("out file does not exist: " + f.getAbsolutePath(), f.exists());
    }


}
