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
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.config.WsConfig;
import org.mule.module.cxf.config.WsSecurity;
import org.mule.module.cxf.support.MuleSecurityManagerValidator;
import org.mule.module.cxf.testmodels.Echo;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.validate.NoOpValidator;
import org.junit.Before;
import org.junit.Test;


public class WebServiceMessageProcessorBuilderTestCase extends AbstractMuleContextTestCase
{
    private WebServiceMessageProcessorBuilder serviceMessageProcessorBuilder;
    private static final String SERVICE_NAME = "Echo";
    private static final String NAMESPACE = "http://cxf.apache.org/";

    @Before
    public void setUp()
    {
        serviceMessageProcessorBuilder = new WebServiceMessageProcessorBuilder();
    }

    @Test
    public void testBuildServiceAttribute() throws MuleException
    {
        serviceMessageProcessorBuilder.setService(SERVICE_NAME);
        serviceMessageProcessorBuilder.setNamespace(NAMESPACE);
        serviceMessageProcessorBuilder.setMuleContext(muleContext);
        serviceMessageProcessorBuilder.setServiceClass(Echo.class);

        CxfInboundMessageProcessor messageProcessor = serviceMessageProcessorBuilder.build();
        assertNotNull(messageProcessor);
        QName serviceName = messageProcessor.getServer().getEndpoint().getService().getName();
        assertEquals(new QName(NAMESPACE, SERVICE_NAME), serviceName);
    }

    @Test
    public void testWsSecurityConfig() throws MuleException
    {
        WsSecurity wsSecurity = new WsSecurity();     
        addConfigProperties(wsSecurity);
        addSecurityManager(wsSecurity);
        addCustomValidator(wsSecurity);
        serviceMessageProcessorBuilder.setWsSecurity(wsSecurity);
        serviceMessageProcessorBuilder.setService(SERVICE_NAME);
        serviceMessageProcessorBuilder.setNamespace(NAMESPACE);
        serviceMessageProcessorBuilder.setMuleContext(muleContext);
        serviceMessageProcessorBuilder.setServiceClass(Echo.class);

        CxfInboundMessageProcessor messageProcessor = serviceMessageProcessorBuilder.build();

        assertNotNull(messageProcessor);
        WSS4JInInterceptor wss4JInInterceptor = getInterceptor(messageProcessor.getServer().getEndpoint().getInInterceptors());
        assertNotNull(wss4JInInterceptor);
        
        Map<String, Object> wss4jProperties = wss4JInInterceptor.getProperties();
        assertFalse(wss4jProperties.isEmpty());
        
        assertEquals(WSHandlerConstants.USERNAME_TOKEN, wss4jProperties.get(WSHandlerConstants.ACTION));
        
        Map<String, Object> properties = serviceMessageProcessorBuilder.getProperties();
        assertNotNull(properties);
        
        assertTrue(properties.get(SecurityConstants.USERNAME_TOKEN_VALIDATOR) instanceof MuleSecurityManagerValidator);
        assertTrue(properties.get(SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR) instanceof NoOpValidator);

    }

    private WSS4JInInterceptor getInterceptor(List<Interceptor<? extends Message>> interceptors)
    {
        for(Interceptor interceptor : interceptors)
        {
            if(interceptor instanceof WSS4JInInterceptor)
            {
                return (WSS4JInInterceptor)interceptor;
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

    private void addSecurityManager(WsSecurity wsSecurity)
    {
        wsSecurity.setSecurityManager(new MuleSecurityManagerValidator());
    }
    
    private void addCustomValidator(WsSecurity wsSecurity)
    {
        Map<String, Object> customValidator = new HashMap<String, Object>();
        customValidator.put(SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR, new NoOpValidator());

        wsSecurity.setCustomValidator(customValidator);
    }


}
