/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.http.HttpConnector;
import org.mule.umo.UMOMessage;

public class HttpBadEncodingFunctionalTestCase extends HttpEncodingFunctionalTestCase
{

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        
        // Send as bytes so that the StringRequestEntity isn't used. If it is used
        // it will throw an exception and stop us from testing the server side.
        MuleMessage msg = new MuleMessage(TEST_MESSAGE.getBytes());
        msg.setEncoding("UTFF-912");
        UMOMessage reply = client.send("clientEndpoint", msg);
        assertNotNull(reply);
        assertEquals("500", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertNotNull(reply.getExceptionPayload());
    }

}
