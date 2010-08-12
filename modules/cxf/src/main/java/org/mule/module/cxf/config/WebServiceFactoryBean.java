package org.mule.module.cxf.config;

import org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder;

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
