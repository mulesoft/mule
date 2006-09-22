/*
 * $Id:TextFileExample.java 2854 2006-08-29 22:49:34 +0000 (Tue, 29 Aug 2006) tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.scripting;

import java.io.File;

import junit.framework.TestResult;
import junit.framework.TestSuite;

public class TextFileExample extends SimpleRunner
 {
    public TextFileExample() {
        super("TextFileExample.xml");
    }

    public static void main(String[] args) {
        TestSuite suite = new TestSuite(TextFileExample.class);
        TestResult result = new TestResult();
        suite.run(result);
    }

    protected void runSample() throws Exception {
        File f = new File("./test-data/out/test");
        assertTrue("file does not exist: " + f.getAbsolutePath(), f.exists());
        assertTrue("Could not move file from out to in", f.renameTo(new File("./test-data/in/test")));

        Thread.sleep(2000);
        assertTrue("out file does not exist: " + f.getAbsolutePath(), f.exists());
    }
}
