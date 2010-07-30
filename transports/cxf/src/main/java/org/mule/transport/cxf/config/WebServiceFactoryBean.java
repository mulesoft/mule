package org.mule.transport.cxf.config;

import org.mule.transport.cxf.builder.WebServiceMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class WebServiceFactoryBean extends WebServiceMessageProcessorBuilder implements FactoryBean
{

    public Object getObject() throws Exception
    {
        return new FlowConfiguringMessageProcessor(this);
    }

    public Class getObjectType()
    {
        return FlowConfiguringMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
