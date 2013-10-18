/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.api.lifecycle.CreateException;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.i18n.CxfMessages;
import org.mule.module.cxf.support.CxfUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;

/**
 * Builds a JAX-WS client based {@link CxfOutboundMessageProcessor}. There 
 * are two ways to configure the client:
 * <ol>
 * <li>WSDL generated client: using the CXF wsdl2java tool, you can configure
 * this biulder using the clientClass, port and wsdlLocation property. The
 * MessageProcessor will then use the generated client proxy to make service invocations.
 * <li>JAX-WS service class: if the serviceClass attribute is specified, this builder
 * will use the {@link JaxWsClientFactoryBean} from CXF to biuld a CXF Client.
 * The MessageProcessor will then use this client instnace to make invocations.
 * </ol>
 * The serviceClass and clientClass attributes are mutually exclusive.
 * @author Dan
 *
 */
public class JaxWsClientMessageProcessorBuilder extends AbstractClientMessageProcessorBuilder
{

    // If we have a proxy we're going to invoke it directly
    // Since the JAX-WS proxy does extra special things for us.
    protected BindingProvider clientProxy;

    protected String clientClass;

    protected String port;

    @Override
    protected Client createClient() throws CreateException, Exception
    {
        if (clientClass != null && serviceClass != null) 
        {
            throw new CreateException(CxfMessages.onlyServiceOrClientClassIsValid(), this);
        }
        
        if (clientClass != null)
        {
            return createClientFromJaxWsProxy();
        }
        else
        {
            return createClientFromFactoryBean();
        }
    }

    private Client createClientFromFactoryBean()
    {
        JaxWsClientFactoryBean cpf = new JaxWsClientFactoryBean();
        cpf.setServiceClass(serviceClass);
        if (databinding == null) 
        {
            cpf.setDataBinding(databinding);
        }
        cpf.setAddress(getAddress());
        cpf.setBus(getBus());
        cpf.setProperties(properties);

        // If there's a soapVersion defined then the corresponding bindingId will be set
        if(soapVersion != null)
        {
            cpf.setBindingId(CxfUtils.getBindingIdForSoapVersion(soapVersion));
        }

        if (wsdlLocation != null)
        {
            cpf.setWsdlURL(wsdlLocation);
        }

        return cpf.create();
    }

    private Client createClientFromJaxWsProxy()
        throws ClassNotFoundException, NoSuchMethodException, IOException, CreateException,
        InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class<?> clientCls = ClassLoaderUtils.loadClass(clientClass, getClass());

        Service s = null;
        if (wsdlLocation != null)
        {
            Constructor<?> cons = clientCls.getConstructor(URL.class, QName.class);
            ResourceManager rr = getBus().getExtension(ResourceManager.class);
            URL url = rr.resolveResource(wsdlLocation, URL.class);

            if (url == null)
            {
                URIResolver res = new URIResolver(wsdlLocation);

                if (!res.isResolved())
                {
                    throw new CreateException(CxfMessages.wsdlNotFound(wsdlLocation), this);
                }
                url = res.getURL();
            }

            WebServiceClient clientAnn = clientCls.getAnnotation(WebServiceClient.class);
            QName svcName = new QName(clientAnn.targetNamespace(), clientAnn.name());

            s = (Service) cons.newInstance(url, svcName);
        }
        else
        {
            s = (Service) clientCls.newInstance();
        }

        if (port == null)
        {
            throw new CreateException(CxfMessages.mustSpecifyPort(), this);
        }

        clientProxy = null;
        for (Method m : clientCls.getMethods())
        {
            WebEndpoint we = m.getAnnotation(WebEndpoint.class);

            if (we != null && we.name().equals(port) && m.getParameterTypes().length == 0)
            {
                clientProxy = (BindingProvider) m.invoke(s, new Object[0]);
                break;
            }
        }

        if (clientProxy == null)
        {
            throw new CreateException(CxfMessages.portNotFound(port), this);
        }

        return ClientProxy.getClient(clientProxy);
    }

    @Override
    protected void configureMessageProcessor(CxfOutboundMessageProcessor processor)
    {
        super.configureMessageProcessor(processor);
        processor.setClientProxy(clientProxy);
    }

    public String getClientClass()
    {
        return clientClass;
    }

    public void setClientClass(String clientClass)
    {
        this.clientClass = clientClass;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }
}
