/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;


import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

public class FileToTcpStreamingTestCase extends FunctionalTestCase
{
    // @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteDirectory(FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory() + "/test-data"));
    }

    // @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/streaming/file-to-tcp-streaming.xml";
    }

    public void testStreamingFromFileToTcp() throws Exception
    {
        String text = "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah " +
                "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah " +
                "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah " +
                "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah\n\n";

        String basepath = muleContext.getConfiguration().getWorkingDirectory() + "/test-data";
        
        FileUtils.stringToFile(basepath + "/in/foo.txt", text);

        Thread.sleep(4000);

        File file = FileUtils.newFile(basepath, "out/foo.txt.processed");
        System.out.println("reading " + file.getAbsolutePath());
        String result = FileUtils.readFileToString(file, "UTF8");
        assertEquals(text, result);
    }
}
