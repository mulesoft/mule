/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class MultipleConnectorsMule1765TestCase extends FunctionalTestCase
{
//    protected static String TEST_SSL_MESSAGE = "Test SSL Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    protected static String TEST_SSL_MESSAGE = "Test SSL Request";

    protected String getConfigResources()
    {
        return "multiple-connectors-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("clientEndpoint", TEST_SSL_MESSAGE, null);
        assertEquals(TEST_SSL_MESSAGE + " Received", result.getPayloadAsString());
    }
}