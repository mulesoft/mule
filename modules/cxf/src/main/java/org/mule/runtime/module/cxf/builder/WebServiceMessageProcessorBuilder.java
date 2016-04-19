/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.builder;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.CxfInboundMessageProcessor;
import org.mule.runtime.module.cxf.MuleJAXWSInvoker;
import org.mule.runtime.module.cxf.i18n.CxfMessages;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.invoker.Invoker;

/**
 * Builds a CXF web service MessageProcessor using either the JAX-WS or
 * simple frontends.  It must be configured in the following way:
 * <ul>
 * <li>If it is part of a {@link org.mule.runtime.core.construct.Flow}, then the serviceClass
 * attribute must be supplied.</li>
 * <li>The builder will use the JAX-WS frontend by default.</li>
 */
public class WebServiceMessageProcessorBuilder
    extends AbstractInboundMessageProcessorBuilder implements FlowConstructAware
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private DataBinding databinding;
    private String frontend = CxfConstants.JAX_WS_FRONTEND;
    private FlowConstruct flowConstruct;
    private Class<?> serviceClass;
    
    @Override
    protected ServerFactoryBean createServerFactory() throws Exception
    {
        ServerFactoryBean sfb;
        if (CxfConstants.SIMPLE_FRONTEND.equals(frontend))
        {
            sfb = new ServerFactoryBean();
            sfb.setDataBinding(new AegisDatabinding());
        }
        else if (CxfConstants.JAX_WS_FRONTEND.equals(frontend))
        {
            sfb = new JaxWsServerFactoryBean();
        }
        else
        {
            throw new CreateException(CxfMessages.invalidFrontend(frontend), this);
        }

        if (serviceClass == null)
        {
            throw new DefaultMuleException(CxfMessages.serviceClassRequiredWithPassThrough());
        }
        sfb.setServiceClass(serviceClass);

        logger.info("Built CXF Inbound MessageProcessor for service class " + serviceClass.getName());

        // Configure Databinding
        if (databinding != null)
        {
            sfb.setDataBinding(databinding);
        }

        if(getService() != null && getNamespace() != null)
        {
            sfb.setServiceName(new QName(getNamespace(), getService()));
        }

        return sfb;
    }

    @Override
    protected Invoker createInvoker(CxfInboundMessageProcessor processor)
    {
        Invoker invoker = super.createInvoker(processor);
        if (CxfConstants.JAX_WS_FRONTEND.equals(frontend))
        {
            invoker = new MuleJAXWSInvoker(invoker);
        }
        
        return invoker;
    }

    @Override
    protected String getAddress()
    {
        if (flowConstruct != null)
        {
            MessageSource source = ((Pipeline) flowConstruct).getMessageSource();

            if (source instanceof InboundEndpoint)
            {
                return ((InboundEndpoint) source).getEndpointURI().toString();
            }
        }
        return "http://internalMuleCxfRegistry/" + hashCode();
    }

    @Override
    public boolean isProxy()
    {
        return false;
    }

    @Override
    public Class<?> getServiceClass()
    {
        return serviceClass;
    }

    public void setServiceClass(Class<?> serviceClass)
    {
        this.serviceClass = serviceClass;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
    public String getFrontend()
    {
        return frontend;
    }

    /**
     * Whether to use the simple frontend or JAX-WS frontend. Valid values
     * are "simple" or "jaxws".
     * @param frontend
     */
    public void setFrontend(String frontend)
    {
        this.frontend = frontend;
    }

    public DataBinding getDatabinding()
    {
        return databinding;
    }

    public void setDatabinding(DataBinding databinding)
    {
        this.databinding = databinding;
    }

}
