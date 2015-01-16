/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.file.FileExists;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

public class FileToTcpStreamingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/streaming/file-to-tcp-streaming-flow.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteDirectory(FileUtils.newFile(getWorkingDirectory() + "/test-data"));
    }

    @Test
    public void testStreamingFromFileToTcp() throws Exception
    {
        String text = "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah\n\n";

        String basepath = getWorkingDirectory() + "/test-data";

        FileUtils.stringToFile(basepath + "/in/foo.txt", text);

        File file = FileUtils.newFile(basepath, "out/foo.txt.processed");

        PollingProber pollingProber = new PollingProber(5000, 10);
        pollingProber.check(new FileExists(file));

        String result = FileUtils.readFileToString(file, "UTF8");
        assertEquals(text, result);
    }
}
