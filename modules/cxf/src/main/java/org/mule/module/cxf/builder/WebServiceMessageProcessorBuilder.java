/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.construct.Pipeline;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.source.MessageSource;
import org.mule.module.cxf.CxfConstants;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.MuleJAXWSInvoker;
import org.mule.module.cxf.i18n.CxfMessages;
import org.mule.service.ServiceCompositeMessageSource;

import java.util.List;

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
 * <li>If the builder is part of a {@link Service}, then it will try to
 * detect the serviceClass from the component.</li>
 * <li>If it is not part of a {@link Service}, then the serviceClass
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
    private Service muleService;
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
            serviceClass = getTargetClass(muleService);
        }
        sfb.setServiceClass(serviceClass);

        logger.info("Built CXF Inbound MessageProcessor for service class " + serviceClass.getName());

        // Configure Databinding
        if (databinding != null)
        {
            sfb.setDataBinding(databinding);
        }

        if (muleService != null && muleService.getComponent() instanceof JavaComponent)
        {
            sfb.setServiceBean(((JavaComponent) muleService.getComponent()).getObjectFactory().getInstance(muleContext));
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

    /**
     * Try to determine the target class from the Service.
     */
    protected Class<?> getTargetClass(Service service) throws MuleException, ClassNotFoundException
    {
        if (service == null)
        {
            throw new DefaultMuleException(CxfMessages.serviceClassRequiredWithPassThrough());
        }

        Component component = service.getComponent();
        if (!(component instanceof JavaComponent))
        {
            throw new DefaultMuleException(CxfMessages.serviceClassRequiredWithPassThrough());
        }

        try
        {
            return ((JavaComponent) component).getObjectType();
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    @Override
    protected String getAddress()
    {
        if (flowConstruct != null)
        {
            if (flowConstruct instanceof Service)
            {
                MessageSource source = ((Service) flowConstruct).getMessageSource();

                if (source instanceof InboundEndpoint)
                {
                    return ((InboundEndpoint) source).getEndpointURI().toString();
                }
                else if (source instanceof ServiceCompositeMessageSource)
                {
                    List<InboundEndpoint> endpoints = ((ServiceCompositeMessageSource) muleService.getMessageSource()).getEndpoints();

                    if (endpoints.size() > 0)
                    {
                        return endpoints.get(0).getEndpointURI().toString();
                    }
                }
            }
            else if (flowConstruct instanceof Pipeline)
            {
                MessageSource source = ((Pipeline) flowConstruct).getMessageSource();

                if (source instanceof InboundEndpoint)
                {
                    return ((InboundEndpoint) source).getEndpointURI().toString();
                }
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

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;

        if (flowConstruct instanceof Service)
        {
            this.muleService = (Service) flowConstruct;
        }
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
