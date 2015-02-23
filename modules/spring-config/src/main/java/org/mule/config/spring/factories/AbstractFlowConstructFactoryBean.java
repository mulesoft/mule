/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.source.MessageSource;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.construct.builder.AbstractFlowConstructWithSingleInboundEndpointBuilder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractFlowConstructFactoryBean implements FactoryBean<FlowConstruct>, ApplicationContextAware, MuleContextAware
{

    protected ApplicationContext applicationContext;
    protected MuleContext muleContext;

    protected AbstractFlowConstruct flowConstruct;

    public boolean isSingleton()
    {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected abstract AbstractFlowConstructBuilder<? extends AbstractFlowConstructBuilder<?, ?>, ? extends AbstractFlowConstruct> getFlowConstructBuilder();

    public void setName(String name)
    {
        getFlowConstructBuilder().name(name);
    }

    public void setInitialState(String initialState)
    {
        getFlowConstructBuilder().initialState(initialState);
    }

    public void setMessageSource(MessageSource messageSource)
    {
        final AbstractFlowConstructBuilder<?, ?> flowConstructBuilder = getFlowConstructBuilder();

        if ((flowConstructBuilder instanceof AbstractFlowConstructWithSingleInboundEndpointBuilder<?, ?>)
            && (messageSource instanceof InboundEndpoint))
        {
            ((AbstractFlowConstructWithSingleInboundEndpointBuilder<?, ?>) flowConstructBuilder).inboundEndpoint((InboundEndpoint) messageSource);
        }
        else
        {
            flowConstructBuilder.messageSource(messageSource);
        }
    }

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        getFlowConstructBuilder().exceptionStrategy(exceptionListener);
    }

    public FlowConstruct getObject() throws Exception
    {
        if (flowConstruct == null)
        {
            flowConstruct = createFlowConstruct();
        }
        return flowConstruct;
    }

    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return getFlowConstructBuilder().build(muleContext);
    }
}
