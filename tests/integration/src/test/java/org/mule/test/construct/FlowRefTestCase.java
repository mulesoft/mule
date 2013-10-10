/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

public class FlowRefTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-ref.xml";
    }   

    @Test
    public void testTwoFlowRefsToSubFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msg = client.send("vm://two.flow.ref.to.sub.flow",
            new DefaultMuleMessage("0", muleContext));
        
        assertEquals("012xyzabc312xyzabc3", msg.getPayloadAsString());        
        
    }

}
