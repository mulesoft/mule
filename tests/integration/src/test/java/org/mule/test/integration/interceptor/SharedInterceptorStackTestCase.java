/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.FunctionalTestCase;

public class SharedInterceptorStackTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "shared-interceptor-stack.xml";
    }
    
    public void testSharedInterceptorOnFlowOne() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        
        MuleMessage response = client.send("vm://stackOne", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " CustomInterceptor One", response.getPayload());
    }
    
    public static class CustomInterceptor implements Interceptor
    {
        public void setListener(MessageProcessor listener)
        {
            // do nothing
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();
            String payload = message.getPayload().toString();
            message.setPayload(payload + " CustomInterceptor");
            return event;
        }
    }
}


