/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;


import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileToTcpStreamingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/streaming/file-to-tcp-streaming.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteDirectory(FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory() + "/test-data"));
    }

    @Test
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
