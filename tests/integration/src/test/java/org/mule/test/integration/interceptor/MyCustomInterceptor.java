/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.interceptor.Interceptor;
import org.mule.processor.AbstractInterceptingMessageProcessor;

public class MyCustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor
{
    
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        String payload = (String)event.getMessage().getPayload();
        event.getMessage().setPayload(payload + "!");
        return processNext(event);
    }

}


