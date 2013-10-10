/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.config;

import org.mule.module.cxf.builder.ProxyServiceMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class ProxyServiceFactoryBean extends ProxyServiceMessageProcessorBuilder implements FactoryBean
{

    public Object getObject() throws Exception
    {
        return new FlowConfiguringMessageProcessor(this);
    }

    public Class<?> getObjectType()
    {
        return FlowConfiguringMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
