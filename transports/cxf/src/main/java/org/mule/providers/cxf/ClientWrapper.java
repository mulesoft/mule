/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.MuleProperties;
import org.mule.providers.cxf.i18n.CxfMessages;
import org.mule.providers.cxf.support.MuleHeadersInInterceptor;
import org.mule.providers.cxf.support.MuleHeadersOutInterceptor;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.DispatchException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

public class ClientWrapper
{

    protected UMOImmutableEndpoint endpoint;
    protected Bus bus;
    protected Client client;
    protected String defaultMethodName;

    // If we have a proxy we're going to invoke it directly
    // Since the JAX-WS proxy does extra special things for us.
    protected BindingProvider proxy;
    protected Method defaultMethod;

    public Client getClient()
    {
        return client;
    }

    public BindingProvider getProxy()
    {
        return proxy;
    }

    public void initialize() throws Exception, IOException
    {
        String clientClass = (String) endpoint.getProperty(CxfConstants.CLIENT_CLASS);
        if (clientClass != null)
        {
            createClientFromClass(bus, clientClass);
        }
        else
        {
            createClientFromLocalServer(bus);
        }
    }

    protected Method findMethod(Class<?> clientCls) throws Exception
    {
        if (defaultMethod == null)
        {
            String op = (String) endpoint.getProperties().get(CxfConstants.OPERATION);
            if (op == null)
            {
                op = (String) endpoint.getProperties().get(CxfConstants.OPERATION);
            }

            if (op != null)
            {
                return getMethodFromOperation(op);
            }
        }

        return null;
    }

    protected BindingOperationInfo getOperation(String opName) throws Exception
    {
        // Normally its not this hard to invoke the CXF Client, but we're
        // sending along some exchange properties, so we need to use a more advanced
        // method
        Endpoint ep = client.getEndpoint();
        QName q = new QName(ep.getService().getName().getNamespaceURI(), opName);
        BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
        if (bop == null)
        {
            throw new Exception("No such operation: " + defaultMethod);
        }

        if (bop.isUnwrappedCapable())
        {
            bop = bop.getUnwrappedOperation();
        }
        return bop;
    }

    private Method getMethodFromOperation(String op) throws Exception
    {
        BindingOperationInfo bop = getOperation(op);
        MethodDispatcher md = (MethodDispatcher) client.getEndpoint().getService().get(
            MethodDispatcher.class.getName());
        return md.getMethod(bop);
    }

    protected void createClientFromClass(Bus bus, String clientClassName) throws Exception
    {
        // TODO: Specify WSDL
        String wsdlLocation = (String) endpoint.getProperty(CxfConstants.WSDL_LOCATION);
        Class<?> clientCls = ClassLoaderUtils.loadClass(clientClassName, getClass());

        Service s = null;
        if (wsdlLocation != null)
        {
            Constructor<?> cons = clientCls.getConstructor(URL.class, QName.class);
            ResourceManager rr = bus.getExtension(ResourceManager.class);
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
        String port = (String) endpoint.getProperty(CxfConstants.CLIENT_PORT);

        if (port == null)
        {
            throw new CreateException(CxfMessages.mustSpecifyPort(), this);
        }

        proxy = null;
        if (port != null)
        {
            for (Method m : clientCls.getMethods())
            {
                WebEndpoint we = m.getAnnotation(WebEndpoint.class);

                if (we != null && we.name().equals(port))
                {
                    proxy = (BindingProvider) m.invoke(s, new Object[0]);
                    break;
                }
            }
        }

        if (proxy == null)
        {
            throw new CreateException(CxfMessages.portNotFound(port), this);
        }

        UMOEndpointURI uri = endpoint.getEndpointURI();
        if (uri.getUser() != null)
        {
            proxy.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, uri.getUser());
        }

        if (uri.getPassword() != null)
        {
            proxy.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, uri.getPassword());
        }

        proxy.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, uri.getAddress());

        client = ClientProxy.getClient(proxy);

        defaultMethod = findMethod(clientCls);
        defaultMethodName = getDefaultMethodName();
    }

    private String getDefaultMethodName()
    {
        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        String m = (String) endpointUri.getParams().get(MuleProperties.MULE_METHOD_PROPERTY);

        if (m == null)
        {
            m = (String) endpoint.getProperties().get(MuleProperties.MULE_METHOD_PROPERTY);
        }

        return m;
    }

    protected void createClientFromLocalServer(final Bus bus) throws Exception, IOException
    {
        String uri = endpoint.getEndpointURI().toString();
        int idx = uri.indexOf('?');
        if (idx != -1)
        {
            uri = uri.substring(0, idx);
        }

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm.getDestinationFactoryForUri(uri);
        if (df == null)
        {
            throw new Exception("Could not find a destination factory for uri " + uri);
        }

        Destination dest = df.getDestination(ei);
        MessageObserver mo = dest.getMessageObserver();
        if (mo instanceof ChainInitiationObserver)
        {
            ChainInitiationObserver cMo = (ChainInitiationObserver) mo;
            Endpoint cxfEP = cMo.getEndpoint();

            client = new ClientImpl(bus, cxfEP);
            client.getInInterceptors().add(new MuleHeadersInInterceptor());
            client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
            client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }
        else
        {
            throw new Exception("Could not create client! No Server was found directly on the endpoint: "
                                + uri);
        }
    }

    protected String getMethodOrOperationName(UMOEvent event) throws DispatchException
    {
        // @TODO: Which of these *really* matter?
        String method = (String) event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);

        if (method == null)
        {
            method = (String) event.getMessage().getProperty(CxfConstants.OPERATION);
        }

        if (method == null)
        {
            method = defaultMethodName;
        }

        if (method == null)
        {
            throw new DispatchException(SoapMessages.cannotInvokeCallWithoutOperation(), event.getMessage(),
                event.getEndpoint());
        }

        return method;
    }

    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public void setBus(Bus bus)
    {
        this.bus = bus;
    }

    public boolean isProxy()
    {
        return proxy != null;
    }

    public BindingOperationInfo getOperation(UMOEvent event) throws Exception
    {
        String opName = getMethodOrOperationName(event);

        if (opName == null)
        {
            opName = defaultMethodName;
        }

        return getOperation(opName);
    }

    public Method getMethod(UMOEvent event) throws Exception
    {
        Method method = defaultMethod;
        if (method == null)
        {
            String opName = (String) event.getMessage().getProperty(CxfConstants.OPERATION);
            if (opName != null) 
            {
                method = getMethodFromOperation(opName);
            }

            if (method == null)
            {
                opName = (String) endpoint.getProperty(CxfConstants.OPERATION);
                if (opName != null) 
                {
                    method = getMethodFromOperation(opName);
                }
            }
            
            if (method == null)
            {
                opName = defaultMethodName;
                if (opName != null) 
                {
                    method = getMethodFromOperation(opName);
                }
            }
        }

        if (method == null)
        {
            throw new DispatchException(CxfMessages.noOperationWasFoundOrSpecified(), event.getMessage(),
                endpoint);
        }
        return method;
    }
}
