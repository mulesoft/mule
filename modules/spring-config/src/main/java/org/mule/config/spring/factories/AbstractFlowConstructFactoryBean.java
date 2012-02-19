/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.source.MessageSource;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.construct.builder.AbstractFlowConstructWithSingleInboundEndpointBuilder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractFlowConstructFactoryBean implements FactoryBean<FlowConstruct>, 
    InitializingBean, ApplicationContextAware, MuleContextAware, Initialisable
{
    private static final NullFlowConstruct NULL_FLOW_CONSTRUCT = new NullFlowConstruct("noop", null);

    /*
     * Shameful hack, read FIXME below
     */
    private static final class NullFlowConstruct extends AbstractFlowConstruct
    {
        public NullFlowConstruct(String name, MuleContext muleContext)
        {
            super(name, muleContext);
        }

        @Override
        public String getConstructType()
        {
            return "NULL";
        }
    }

    protected ApplicationContext applicationContext;
    protected MuleContext muleContext;

    // FIXME terrible hack to get around the first call to getObject that comes too
    // soon (nothing is injected yet)
    protected AbstractFlowConstruct flowConstruct = NULL_FLOW_CONSTRUCT;

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

    public void afterPropertiesSet() throws Exception
    {
        flowConstruct = createFlowConstruct();
    }

    public void initialise() throws InitialisationException
    {
        flowConstruct.initialise();
    }

    public FlowConstruct getObject() throws Exception
    {
        return flowConstruct;
    }

    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return getFlowConstructBuilder().build(muleContext);
    }
}
