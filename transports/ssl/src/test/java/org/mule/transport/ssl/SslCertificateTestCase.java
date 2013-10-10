/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SslCertificateTestCase extends FunctionalTestCase
{

    private static int NUM_MESSAGES = 100;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "ssl-certificate-test.xml";
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

    protected void doTests(int n) throws Exception
    {
        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("service");
        assertNotNull(ftc);
        assertNotNull(ftc.getEventCallback());

        SaveCertificateCallback callback = (SaveCertificateCallback) ftc.getEventCallback();
        callback.clear();

        MuleClient client = new MuleClient(muleContext);
        for (int i = 0; i < n; ++i)
        {
            callback.clear();
            String msg = TEST_MESSAGE + n;
            MuleMessage result = client.send("in", msg, null);
            assertTrue(callback.isCalled());
            assertNotNull("Null certificates", callback.getCertificates());
            assertEquals(msg + " Received", result.getPayloadAsString());
        }
    }
}
