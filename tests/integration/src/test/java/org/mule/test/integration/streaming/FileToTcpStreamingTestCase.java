/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;


import org.mule.RegistryContext;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

public class FileToTcpStreamingTestCase extends FunctionalTestCase
{
    // @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteDirectory(FileUtils.newFile(RegistryContext.getConfiguration().getWorkingDirectory() + "/test-data"));
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

        String basepath = RegistryContext.getConfiguration().getWorkingDirectory() + "/test-data";
        FileUtils.stringToFile(basepath + "/in/foo.txt", text);

        Thread.sleep(3000);

        String result = FileUtils.readFileToString(FileUtils.newFile(basepath, "out/foo.txt.processed"), "UTF8");
        assertEquals(text, result);
    }
}
