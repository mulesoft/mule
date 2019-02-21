/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;

/**
 * We are careful here to access the file system in a generic way. This means setting
 * directories dynamically.
 */
public abstract class AbstractFileFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String TEST_MESSAGE = "Test file contents";
    public static final String TARGET_FILE = "TARGET_FILE";

    private static final Logger LOGGER = getLogger(AbstractFileFunctionalTestCase.class);

    protected File tmpDir;

    public AbstractFileFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "file-functional-test-service.xml"},
            {ConfigVariant.FLOW, "file-functional-test-flow.xml"}
        });
    }      
    
    protected String fileToUrl(File file) throws MalformedURLException
    {
        return file.getAbsoluteFile().toURI().toURL().toString();
    }

    // annoying but necessary wait apparently due to OS caching?
    protected void waitForFileSystem() throws Exception
    {
        synchronized (this)
        {
            wait(1000);
        }
    }

    protected File initForRequest() throws Exception
    {
        createTempDirectory();
        File target = createAndPopulateTempFile("mule-file-test-", ".txt");

        // define the readFromDirectory on the connector
        FileConnector connector = (FileConnector) muleContext.getRegistry().lookupConnector(
            "receiveConnector");
        connector.setReadFromDirectory(tmpDir.getAbsolutePath());
        LOGGER.debug("Directory is " + connector.getReadFromDirectory());

        waitForFileSystem();
        return target;
    }

    private void createTempDirectory() throws Exception
    {
        tmpDir = File.createTempFile("mule-file-test-", "-dir");
        FileUtils.deleteFile(tmpDir);
        tmpDir.mkdir();
    }

    protected File createAndPopulateTempFile(String prefix, String suffix) throws Exception
    {
        File target = File.createTempFile(prefix, suffix, tmpDir);
        LOGGER.info("Created temporary file: " + target.getAbsolutePath());

        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();

        target.deleteOnExit();
        return target;
    }

    protected void checkReceivedMessage(MuleMessage message) throws Exception
    {
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof InputStream);

        InputStream fis = (InputStream) message.getPayload();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        IOUtils.copy(fis, byteOut);
        fis.close();
        String result = new String(byteOut.toByteArray());
        assertEquals(TEST_MESSAGE, result);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(tmpDir);
    }

}
