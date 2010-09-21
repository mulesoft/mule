/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.endpoints;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.FunctionalTestCase;

/**
 *
 */
public class DynamicEndpointConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-config.xml";
    }

    public void testName() throws Exception
    {

        MuleMessage msg = new DefaultMuleMessage("Data", muleContext);
        msg.setOutboundProperty("testProp", "testPath");
        MuleMessage response = muleContext.getClient().send("vm://in1", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));

        response = muleContext.getClient().send("vm://in2", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));
    }
}
