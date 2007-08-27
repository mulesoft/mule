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
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class HttpBadEncodingFunctionalTestCase extends HttpEncodingFunctionalTestCase
{

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map messageProperties = new HashMap();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTFF-912");
        UMOMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("500", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertNotNull(reply.getExceptionPayload());
    }

}
