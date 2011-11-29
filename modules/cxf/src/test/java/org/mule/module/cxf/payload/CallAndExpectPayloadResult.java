/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.payload;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import static org.junit.Assert.assertEquals;

class CallAndExpectPayloadResult implements CallAndExpect
{
    private Object expectedPayloadResult;
    private String outputEndpointName;
    private Object payload;
    private final MuleContext muleContext;

    public CallAndExpectPayloadResult(String outputEndpointName,
                                      Object payload,
                                      Object expectedPayloadResult,
                                      MuleContext muleContext)
    {
        this.expectedPayloadResult = expectedPayloadResult;
        this.outputEndpointName = outputEndpointName;
        this.payload = payload;
        this.muleContext = muleContext;
    }

    public void callEndpointAndExecuteAsserts() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send(outputEndpointName, payload, null);

        assertEquals(here(), expectedPayloadResult, result.getPayload());
    }

    private String here()
    {
        return "In [" + outputEndpointName + "," + payload + "," + expectedPayloadResult + "]";
    }
}
