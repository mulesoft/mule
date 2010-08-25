/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class HttpPollingFunctionalTestCase extends FunctionalTestCase
{

    public void testPollingHttpConnector() throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("polled");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback(){
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                assertEquals("The Accept header should be set on the incoming message", "application/xml", context.getMessage().<String>getInboundProperty("Accept"));
            }
        });
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request("vm://toclient", RECEIVE_TIMEOUT);
        assertNotNull(result.getPayload());
        assertEquals("foo", result.getPayloadAsString());
    }
    
    protected String getConfigResources()
    {
        return "mule-http-polling-config.xml";
    }

}
