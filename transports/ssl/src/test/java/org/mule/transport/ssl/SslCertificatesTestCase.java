/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Iterator;

/**
 * A different version of {@link org.mule.transport.ssl.SslCertificateTestCase} to see if we can get
 * different timing.
 */
public class SslCertificatesTestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test Request";
    private static int NUM_MESSAGES = 100;

    protected String getConfigResources()
    {
        return "ssl-certificates-test.xml";
    }

    public void testOnce() throws Exception
    {
        doTests(1);
    }

    public void testMany() throws Exception
    {
        doTests(NUM_MESSAGES);
    }

    protected void doTests(int n) throws Exception
    {
        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("service");
        assertNotNull(ftc);
        assertNotNull(ftc.getEventCallback());

        SaveCertificatesCallback callback = (SaveCertificatesCallback) ftc.getEventCallback();
        callback.clear();

        MuleClient client = new MuleClient();
        for (int i = 0; i < n; ++i)
        {
            String msg = TEST_MESSAGE + n;
            MuleMessage result = client.send("in", msg, null);
            assertEquals(msg  + " Received", result.getPayloadAsString());
        }
        Iterator certificates = callback.getCertificates().iterator();
        for (int i = 0; i < n; ++i)
        {
            assertTrue("No cert at " + i, certificates.hasNext());
            assertNotNull("Null cert at " + i, certificates.next());
        }
    }

}