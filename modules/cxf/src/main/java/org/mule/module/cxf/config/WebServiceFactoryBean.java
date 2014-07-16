/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.config;

import org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class WebServiceFactoryBean extends WebServiceMessageProcessorBuilder implements FactoryBean
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
