/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.config;

import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.JaxWsClientMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class JaxWsClientFactoryBean extends JaxWsClientMessageProcessorBuilder implements FactoryBean
{

    public Object getObject() throws Exception
    {
        return build();
    }

    public Class<?> getObjectType()
    {
        return CxfOutboundMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
