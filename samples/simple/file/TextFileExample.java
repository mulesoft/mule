/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package file;

import util.SimpleRunner;

import java.io.File;

import http.BinaryHttpExample;
import junit.framework.TestSuite;
import junit.framework.TestResult;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
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
