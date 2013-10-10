/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

/**
 * A different version of {@link org.mule.transport.ssl.SslCertificateTestCase} to see if we can get
 * different timing.
 */
public class SslCertificatesTestCase extends AbstractServiceAndFlowTestCase
{
    private static int NUM_MESSAGES = 100;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public SslCertificatesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
        
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "ssl-certificates-test-service.xml"},
            {ConfigVariant.FLOW, "ssl-certificates-test-flow.xml"}});
    }

    @Test
    public void testOnce() throws Exception
    {
        doTests(1);
    }

    @Test
    public void testMany() throws Exception
    {
        doTests(NUM_MESSAGES);
    }

    protected void doTests(int numberOfMessages) throws Exception
    {
        SaveCertificatesCallback callback = setupEventCallback();

        MuleClient client = new MuleClient(muleContext);
        for (int i = 0; i < numberOfMessages; ++i)
        {
            String msg = TEST_MESSAGE + i;
            MuleMessage result = client.send("in", msg, null);
            assertEquals(msg  + " Received", result.getPayloadAsString());
        }

        Iterator<Certificate[]> certificates = callback.getCertificates().iterator();
        for (int i = 0; i < numberOfMessages; ++i)
        {
            assertTrue("No cert at " + i, certificates.hasNext());
            assertNotNull("Null cert at " + i, certificates.next());
        }
    }

    private SaveCertificatesCallback setupEventCallback() throws Exception
    {
        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("service");
        assertNotNull(ftc);
        assertNotNull(ftc.getEventCallback());

        SaveCertificatesCallback callback = (SaveCertificatesCallback) ftc.getEventCallback();
        callback.clear();
        return callback;
    }
}
