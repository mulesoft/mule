/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.config.WsConfig;
import org.mule.module.cxf.config.WsSecurity;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.api.MuleException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Before;
import org.junit.Test;

public class SimpleClientMessageProcessorBuilderTestCase extends AbstractMuleContextTestCase
{
    private SimpleClientMessageProcessorBuilder simpleClientMessageProcessorBuilder;
    private static final String SERVICE_CLASS = "org.mule.module.cxf.testmodels.Echo";


    @Before
    public void setUp()
    {
        simpleClientMessageProcessorBuilder = new SimpleClientMessageProcessorBuilder();
    }

    @Test
    public void testWsSecurityConfig() throws MuleException
    {
        WsSecurity wsSecurity = new WsSecurity();
        addConfigProperties(wsSecurity);

        simpleClientMessageProcessorBuilder.setWsSecurity(wsSecurity);
        simpleClientMessageProcessorBuilder.setServiceClass(SERVICE_CLASS.getClass());
        simpleClientMessageProcessorBuilder.setMuleContext(muleContext);
        CxfOutboundMessageProcessor messageProcessor = simpleClientMessageProcessorBuilder.build();

        assertNotNull(messageProcessor);
        WSS4JOutInterceptor wss4JOutInterceptor = getInterceptor(messageProcessor.getClient().getOutInterceptors());
        assertNotNull(wss4JOutInterceptor);

        Map<String, Object> wss4jProperties = wss4JOutInterceptor.getProperties();
        assertFalse(wss4jProperties.isEmpty());

        assertEquals(WSHandlerConstants.USERNAME_TOKEN, wss4jProperties.get(WSHandlerConstants.ACTION));

    }

    private WSS4JOutInterceptor getInterceptor(List<Interceptor<? extends Message>> interceptors)
    {
        for(Interceptor interceptor : interceptors)
        {
            if(interceptor instanceof WSS4JOutInterceptor)
            {
                return (WSS4JOutInterceptor)interceptor;
            }
        }
        return null;
    }

    private void addConfigProperties(WsSecurity wsSecurity)
    {
        Map<String, Object> configProperties = new HashMap<String, Object>();
        configProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);

        wsSecurity.setWsConfig(new WsConfig(configProperties));
    }

}
