package org.mule.module.cxf.config;

import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.SimpleClientMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class SimpleClientFactoryBean extends SimpleClientMessageProcessorBuilder implements FactoryBean
{

    public Object getObject() throws Exception
    {
        return build();
    }

    public Class getObjectType()
    {
        return CxfOutboundMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
