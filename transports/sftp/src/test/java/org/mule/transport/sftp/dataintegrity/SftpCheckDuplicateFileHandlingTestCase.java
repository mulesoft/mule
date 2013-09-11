/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.transport.sftp.LatchDownExceptionListener;
import org.mule.transport.sftp.SftpClient;
import org.mule.transport.sftp.util.SftpServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the three different types of handling when duplicate files (i.e. file names)
 * are being transferred by SftpTransport. Available duplicate handling types are: -
 * SftpConnector.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION = "throwException" -
 * SftpConnectorPROPERTY_DUPLICATE_HANDLING_OVERWRITE = "overwrite" (currently not
 * implemented) - SftpConnector.PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO = "addSeqNo"
 */
@Ignore
public class SftpCheckDuplicateFileHandlingTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String DUPLICATED_FILENAME = "file_1.txt";

    private static final String FILENAME = "file.txt";

    private static final String FILENAME_MESSAGE_PROPERTY = "filename";

    private static SftpClient sftpClient;

    @Rule
    public DynamicPort port = new DynamicPort("SFTP_PORT");

    private static SftpServer sftpServer;

    public SftpCheckDuplicateFileHandlingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    private static final HashMap<String, Object> MESSAGE_PROPERTIES = new HashMap<String, Object>();
    {
        MESSAGE_PROPERTIES.put(FILENAME_MESSAGE_PROPERTY, FILENAME);
    }

    private Prober prober = new PollingProber(2000, 100);

    private MuleClient muleClient;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-dataintegrity-duplicate-handling-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-dataintegrity-duplicate-handling-flow.xml"}});
    }

    /**
     * Try to transfer two files with the same name. The second file will be given a
     * new name.
     */
    @Test
    public void testDuplicateChangeNameHandling() throws Exception
    {
        String endpointUrl = "sftp://muletest1:muletest1@localhost:" + port.getNumber() + "/~/inbound";
        muleClient.dispatch(endpointUrl, TEST_MESSAGE, MESSAGE_PROPERTIES);
        prober.check(new SftpFilePresentProbe(sftpClient, "outbound", FILENAME));
        muleClient.dispatch(endpointUrl, TEST_MESSAGE, MESSAGE_PROPERTIES);
        prober.check(new SftpFilePresentProbe(sftpClient, "outbound", DUPLICATED_FILENAME));
    }

    /**
     * Try to transfer two files with the same name. The second dispatch will throw
     * and exception.
     */
    @Test
    public void testDuplicateThrowExceptionHandling() throws Exception
    {
        String endpointUrl = "sftp://muletest1:muletest1@localhost:" + port.getNumber() + "/~/inbound2";
        muleClient.dispatch(endpointUrl, TEST_MESSAGE, MESSAGE_PROPERTIES);
        prober.check(new SftpFilePresentProbe(sftpClient, "outbound2", FILENAME));
        final CountDownLatch latch = new CountDownLatch(1);
        muleContext.registerListener(new LatchDownExceptionListener(latch));
        muleClient.dispatch(endpointUrl, TEST_MESSAGE, MESSAGE_PROPERTIES);
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    /**
     * Returns a SftpClient that is logged in to the sftp server that the endpoint is
     * configured against.
     */
    protected SftpClient getSftpClient(String host, int clientPort, String user, String password)
        throws IOException
    {
        SftpClient client = new SftpClient(host);
        client.setPort(clientPort);
        try
        {
            client.login(user, password);
        }
        catch (Exception e)
        {
            fail("Login failed: " + e);
        }
        return client;
    }

    @Before
    public void before() throws MuleException, IOException
    {
        sftpServer = new SftpServer(port.getNumber());
        sftpServer.start();
        muleClient = muleContext.getClient();
        sftpClient = getSftpClient("localhost", port.getNumber(), "muletest1", "muletest1");
        sftpClient.mkdir("inbound");
        sftpClient.mkdir("outbound");
        sftpClient.mkdir("inbound2");
        sftpClient.mkdir("outbound2");
    }

    @After
    public void after() throws IOException
    {
        sftpClient.recursivelyDeleteDirectory("outbound");
        sftpClient.recursivelyDeleteDirectory("inbound");
        sftpClient.recursivelyDeleteDirectory("outbound2");
        sftpClient.recursivelyDeleteDirectory("inbound2");
        sftpServer.stop();
    }
}
