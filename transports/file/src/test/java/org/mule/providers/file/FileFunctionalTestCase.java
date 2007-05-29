/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.model.UMOModel;
import org.mule.impl.endpoint.MuleEndpoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * We are careful here to access the file sstem in a generic way.  This means setting directories
 * dynamically.
 */
public class FileFunctionalTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test file contents";
    public static final String TARGET_FILE = "TARGET_FILE";

    public FileFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "file-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt");
        target.deleteOnExit();

        FileConnector connector =
                (FileConnector) managementContext.getRegistry().getConnectors().get("sendConnector");
        connector.setWriteToDirectory(target.getParent());
        logger.debug("Directory is " + connector.getWriteToDirectory());
        Map props = new HashMap();
        props.put(TARGET_FILE, target.getName());
        logger.debug("File is " + props.get(TARGET_FILE));

        MuleClient client = new MuleClient();
        client.dispatch("send", TEST_MESSAGE, props);
        waitForFileSystem();

        String result = new BufferedReader(new FileReader(target)).readLine();
        assertEquals(TEST_MESSAGE, result);
    }

    public void testDirectReceive() throws Exception
    {
        File target = initForReceive();
        MuleClient client = new MuleClient();
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        UMOMessage message = client.receive(url, 1000);
        checkReceivedMessage(message);
    }

    protected String fileToUrl(File file) throws MalformedURLException
    {
        return file.getAbsoluteFile().toURI().toURL().toString();
    }

    public void testIndirectReceive() throws Exception
    {
        File target = initForReceive();

        // add a receiver endpoint that will poll the readFromDirectory
        UMOModel model = (UMOModel) managementContext.getRegistry().getModels().get("receiveModel");
        assertNotNull(model);
        UMOComponent relay = model.getComponent("relay");
        assertNotNull(relay);
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        relay.getDescriptor().getInboundRouter().addEndpoint(
	    new MuleEndpoint(url, true));

        // then read from the queue that the polling receiver will write to
        MuleClient client = new MuleClient();
        UMOMessage message = client.receive("receive", 3000);
        checkReceivedMessage(message);
    }

    // annoying but necessary wait apparently due to OS caching?
    private void waitForFileSystem() throws Exception
    {
        synchronized(this)
        {
            wait(1000);
        }
    }

    private File initForReceive() throws Exception
    {
        File tmpDir = File.createTempFile("mule-file-test-", "-dir");
        tmpDir.delete();
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        File target = File.createTempFile("mule-file-test-", ".txt", tmpDir);
        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();
        target.deleteOnExit();

        // define the readFromDirectory on the connector
        FileConnector connector =
                (FileConnector) managementContext.getRegistry().getConnectors().get("receiveConnector");
        connector.setReadFromDirectory(tmpDir.getAbsolutePath());
        logger.debug("Directory is " + connector.getReadFromDirectory());

        waitForFileSystem();
        return target;
    }

    private void checkReceivedMessage(UMOMessage message) throws Exception
    {
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof byte[]);
        String result = new String((byte[]) message.getPayload());
        assertEquals(TEST_MESSAGE, result);
    }

}
