/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.security.cert.Certificate;
import java.util.Iterator;

/**
 * A different version of {@link org.mule.transport.ssl.SslCertificateTestCase} to see if we can get
 * different timing.
 */
public class SslCertificatesTestCase extends DynamicPortTestCase
{
    private static int NUM_MESSAGES = 100;

    @Override
    protected String getConfigResources()
    {
        return "ssl-certificates-test.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testOnce() throws Exception
    {
        doTests(1);
    }

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
