/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * We are careful here to access the file sstem in a generic way.  This means setting directories
 * dynamically.
 */
public class FileFunctionalTestCase extends AbstractFileFunctionalTestCase
{

    public FileFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSend() throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt");
        target.deleteOnExit();

        FileConnector connector =
                (FileConnector) muleContext.getRegistry().lookupConnector("sendConnector");
        connector.setWriteToDirectory(target.getParent());
        Map<String, String> props = new HashMap<String, String>();
        props.put(TARGET_FILE, target.getName());

        MuleClient client = new MuleClient(muleContext);
        client.dispatch("send", TEST_MESSAGE, props);
        waitForFileSystem();

        String result = new BufferedReader(new FileReader(target)).readLine();
        assertEquals(TEST_MESSAGE, result);
    }

    @Test
    public void testDirectRequest() throws Exception
    {
        File target = initForRequest();
        MuleClient client = new MuleClient(muleContext);
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        MuleMessage message = client.request(url, 100000);
        checkReceivedMessage(message);
    }
    
    @Test
    public void testRecursive() throws Exception
    {
        File directory = new File("./.mule/in");
        File subDirectory = new File(directory.getAbsolutePath() + "/sub");
        boolean success = subDirectory.mkdir();
        assertTrue(success);
        subDirectory.deleteOnExit();

        File target = File.createTempFile("mule-file-test-", ".txt", subDirectory);
        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();
        target.deleteOnExit();

        MuleClient client = new MuleClient(muleContext);
        Thread.sleep(1000);
        MuleMessage message = client.request("vm://receive?connector=vmQueue", 100000);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
    }
}
