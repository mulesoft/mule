/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.DispatchException;
import org.mule.transport.cxf.i18n.CxfMessages;
import org.mule.transport.cxf.support.CopyAttachmentInInterceptor;
import org.mule.transport.cxf.support.CopyAttachmentOutInterceptor;
import org.mule.transport.cxf.support.MuleHeadersInInterceptor;
import org.mule.transport.cxf.support.MuleHeadersOutInterceptor;
import org.mule.transport.cxf.support.MuleProtocolHeadersOutInterceptor;
import org.mule.transport.cxf.support.OutputPayloadInterceptor;
import org.mule.transport.cxf.support.ProxyService;
import org.mule.transport.cxf.support.StreamClosingInterceptor;
import org.mule.transport.cxf.transport.MuleUniversalConduit;
import org.mule.transport.soap.i18n.SoapMessages;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
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

    protected ImmutableEndpoint endpoint;
    protected Bus bus;
    protected Client client;
    protected String defaultMethodName;

    // If we have a proxy we're going to invoke it directly
    // Since the JAX-WS proxy does extra special things for us.
    protected BindingProvider clientProxy;
    protected Method defaultMethod;

    protected boolean proxy;
    private boolean applyTransformersToProtocol;
    private boolean enableHeaders;
    
    public Client getClient()
    {
        return client;
    }

    public BindingProvider getClientProxy()
    {
        return clientProxy;
    }

    @SuppressWarnings("unchecked")
    public void initialize() throws Exception
    {
        String clientClass = (String) endpoint.getProperty(CxfConstants.CLIENT_CLASS);
        proxy = BooleanUtils.toBoolean((String) endpoint.getProperty(CxfConstants.PROXY));
        
        if (clientClass != null)
        {
            createClientFromClass(bus, clientClass);
        }
        else if (proxy)
        {
            createClientProxy(bus);
        }
        else
        {
            createClientFromLocalServer(bus);
        }
        
        addInterceptors(client.getInInterceptors(), (List<Interceptor>) endpoint.getProperty(CxfConstants.IN_INTERCEPTORS));
        addInterceptors(client.getInFaultInterceptors(), (List<Interceptor>) endpoint.getProperty(CxfConstants.IN_FAULT_INTERCEPTORS));
        addInterceptors(client.getOutInterceptors(), (List<Interceptor>) endpoint.getProperty(CxfConstants.OUT_INTERCEPTORS));
        addInterceptors(client.getOutFaultInterceptors(), (List<Interceptor>) endpoint.getProperty(CxfConstants.OUT_FAULT_INTERCEPTORS));

        if (proxy)
        {
            client.getInInterceptors().add(new CopyAttachmentInInterceptor());
            client.getInInterceptors().add(new StreamClosingInterceptor());
            client.getOutInterceptors().add(new OutputPayloadInterceptor());
            client.getOutInterceptors().add(new CopyAttachmentOutInterceptor());
            ((MuleUniversalConduit)client.getConduit()).setCloseInput(false);
        }
        
        String value = (String) endpoint.getProperty(CxfConstants.APPLY_TRANSFORMERS_TO_PROTOCOL);
        applyTransformersToProtocol = isTrue(value, true); 
        ((MuleUniversalConduit)client.getConduit()).setApplyTransformersToProtocol(applyTransformersToProtocol);
        
        enableHeaders = isTrue((String) endpoint.getProperty(CxfConstants.ENABLE_MULE_SOAP_HEADERS), true); 
        
        List<AbstractFeature> features = (List<AbstractFeature>) endpoint.getProperty(CxfConstants.OUT_FAULT_INTERCEPTORS);
        
        if (features != null)
        {
            for (AbstractFeature f : features)
            {
                f.initialize(client, bus);
            }
        }

        EndpointImpl ep = (EndpointImpl) client.getEndpoint();
        
        Object mtomEnabled = endpoint.getProperty(CxfConstants.MTOM_ENABLED);
        if (mtomEnabled != null) 
        {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(Message.MTOM_ENABLED, mtomEnabled);
            ep.setProperties(props);
        }
        
        addMuleInterceptors();
    }

    private boolean isTrue(String value, boolean def)
    {
        if (value == null) return def;
        
        return BooleanUtils.toBoolean((String)value);
    }

    @SuppressWarnings("unchecked")
    private void addInterceptors(List<Interceptor> col, List<Interceptor> supplied)
    {
        if (supplied != null) 
        {
            col.addAll(supplied);
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
            throw new Exception("No such operation: " + opName);
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

    protected void createClientProxy(Bus bus) throws Exception
    {
        // TODO: Specify WSDL
        String wsdlLocation = (String) endpoint.getProperty(CxfConstants.WSDL_LOCATION);
        
        ClientProxyFactoryBean cpf = new ClientProxyFactoryBean();
        cpf.setServiceClass(ProxyService.class);
        cpf.setDataBinding(new StaxDataBinding());
        cpf.getFeatures().add(new StaxDataBindingFeature());
        cpf.setAddress(endpoint.getEndpointURI().getAddress());
        cpf.setBus(bus);
        
        if (wsdlLocation != null) 
        {
            cpf.setWsdlLocation(wsdlLocation);
        }
        
        this.client = ClientProxy.getClient(cpf.create());
        
        Binding binding = this.client.getEndpoint().getBinding();
        
        removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
        
        proxy = true;
    }

    @SuppressWarnings("unchecked")
    private void removeInterceptor(List<Interceptor> inInterceptors, String name) {

        for (Interceptor<?> i : inInterceptors) {
            if (i instanceof PhaseInterceptor) {
                PhaseInterceptor<Message> p = (PhaseInterceptor<Message>)i;

                if (p.getId().equals(name)) {
                    inInterceptors.remove(p);
                    return;
                }
            }
        }
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

        clientProxy = null;
        if (port != null)
        {
            for (Method m : clientCls.getMethods())
            {
                WebEndpoint we = m.getAnnotation(WebEndpoint.class);

                if (we != null && we.name().equals(port) && m.getParameterTypes().length == 0)
                {
                    clientProxy = (BindingProvider) m.invoke(s, new Object[0]);
                    break;
                }
            }
        }

        if (clientProxy == null)
        {
            throw new CreateException(CxfMessages.portNotFound(port), this);
        }

        EndpointURI uri = endpoint.getEndpointURI();
        if (uri.getUser() != null)
        {
            clientProxy.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, uri.getUser());
        }

        if (uri.getPassword() != null)
        {
            clientProxy.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, uri.getPassword());
        }

        clientProxy.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, uri.getAddress());

        client = ClientProxy.getClient(clientProxy);

        defaultMethod = findMethod(clientCls);
        defaultMethodName = getDefaultMethodName();
    }

    private String getDefaultMethodName()
    {
        EndpointURI endpointUri = endpoint.getEndpointURI();
        String m = (String) endpointUri.getParams().get(MuleProperties.MULE_METHOD_PROPERTY);

        if (m == null)
        {
            m = (String) endpoint.getProperties().get(MuleProperties.MULE_METHOD_PROPERTY);
        }

        return m;
    }

    protected void createClientFromLocalServer(final Bus bus) throws Exception
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
        }
        else
        {
            throw new Exception("Could not create client! No Server was found directly on the endpoint: "
                                + uri);
        }
    }

    protected void addMuleInterceptors()
    {
        client.getInInterceptors().add(new MuleHeadersInInterceptor());
        client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
        if (enableHeaders)
        {
            client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }
        client.getOutInterceptors().add(new MuleProtocolHeadersOutInterceptor());
        client.getOutFaultInterceptors().add(new MuleProtocolHeadersOutInterceptor());
    }

    protected String getMethodOrOperationName(MuleEvent event) throws DispatchException
    {
        // People can specify a CXF operation, which may in fact be different
        // than the method name. If that's not found, we'll default back to the 
        // mule method property. 
        String method = (String) event.getMessage().getProperty(CxfConstants.OPERATION);

        if (method == null)
        {
            method = (String) event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        }

        if (method == null)
        {
            method = defaultMethodName;
        }
        
        if (method == null && proxy)
        {
            return "invoke";
        }

        if (method == null)
        {
            throw new DispatchException(SoapMessages.cannotInvokeCallWithoutOperation(), event.getMessage(),
                event.getEndpoint());
        }

        return method;
    }

    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public void setBus(Bus bus)
    {
        this.bus = bus;
    }

    public boolean isClientProxyAvailable()
    {
        return clientProxy != null;
    }

    public BindingOperationInfo getOperation(MuleEvent event) throws Exception
    {
        String opName = getMethodOrOperationName(event);

        if (opName == null)
        {
            opName = defaultMethodName;
        }

        return getOperation(opName);
    }

    public Method getMethod(MuleEvent event) throws Exception
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

    public boolean isProxy()
    {
        return proxy;
    }

    public boolean isApplyTransformersToProtocol()
    {
        return applyTransformersToProtocol;
    }
    
}
