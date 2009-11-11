/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.net.SocketTimeoutException;
import java.util.Date;

/**
 * See MULE-4490 "Outbound CXF endpoint does not propagate any properties to "protocol" endpoint"
 */
public class HttpResponseTimeoutTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "http-response-timeout-config.xml";
    }

    public void testResponseTimeout() throws Exception
    {
        MuleClient client = new MuleClient();
        Date beforeCall = new Date();

        MuleMessage message = client.send("vm://request", "<test/>", null);
        assertNotNull(message);

        Date afterCall = new Date();
        // If everything is good the connection will timeout after 5s and throw an exception.
        assertTrue(message.getExceptionPayload().getRootException() instanceof SocketTimeoutException);
        assertTrue((afterCall.getTime() - beforeCall.getTime()) < 10000);
    }

}
