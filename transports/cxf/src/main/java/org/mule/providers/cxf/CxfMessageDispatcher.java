/*
 * $Id: CxfMessageDispatcher.java 6446 2007-05-10 09:33:21Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.cxf.i18n.CxfMessages;
import org.mule.providers.cxf.support.MuleHeadersInInterceptor;
import org.mule.providers.cxf.support.MuleHeadersOutInterceptor;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
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
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

/**
 * The CxfMessageDispatcher is used for making Soap client requests to remote
 * services.
 */
public class CxfMessageDispatcher extends AbstractMessageDispatcher
{

    private static final String URI_REGEX = "cxf:\\[(.+?)\\]:(.+?)/\\[(.+?)\\]:(.+?)";
    Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    // Since the MessageDispatcher is guaranteed to serve a single thread,
    // the Dispatcher can own the xfire Client as an instance variable
    protected Client client = null;
    protected final CxfConnector connector;
    protected String methodName;
    
    // If we have a proxy we're going to invoke it directly
    // Since the JAX-WS proxy does extra special things for us.
    protected BindingProvider proxy;
    protected Method method;
    
    public CxfMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (CxfConnector) endpoint.getConnector();
    }

    /*
    We need a way to associate an endpoint with a specific CXF service and operation, and the most sensible way to
    accomplish that is to overload URI syntax:

    cxf:[service_URI]:service_localname/[ep_URI]:ep_localname

    And the map method to operation
     */
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            final Bus bus = connector.getCxfBus();
            
            createClient(bus);
        }
    }

    protected Method findMethod(Class<?> clientCls) throws Exception
    {
        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        methodName = (String)endpointUri.getParams().get(MuleProperties.MULE_METHOD_PROPERTY);
        
        if (methodName == null)
        {
            methodName = (String)endpoint.getProperties().get(MuleProperties.MULE_METHOD_PROPERTY);
        }   

        if (method == null)
        {
            String op = (String)endpoint.getProperties().get(CxfConstants.OPERATION);
            if (op == null)
            {
                op = (String)endpoint.getProperties().get(CxfConstants.OPERATION);
            }
            
            if (op != null)
            {
                return getMethodFromOperation(op);
            }
        }
        
        return null;
    }

    private Method getMethodFromOperation(String op) throws Exception
    {
        BindingOperationInfo bop = getOperation(op);
        MethodDispatcher md = (MethodDispatcher) 
            client.getEndpoint().getService().get(MethodDispatcher.class.getName());
        return md.getMethod(bop);
    }

    protected void createClient(final Bus bus) throws Exception, IOException 
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

    protected void createClientFromClass(Bus bus, String clientClassName) throws Exception
    {
        // TODO: Specify WSDL
        String wsdlLocation = (String) endpoint.getProperty(CxfConstants.WSDL_LOCATION);
        Class<?> clientCls = ClassLoaderUtils.loadClass(clientClassName, getClass());
        
        Service s = null;
        if (wsdlLocation != null)
        {
            Constructor cons = clientCls.getConstructor(URL.class, QName.class);
            ResourceManager rr = bus.getExtension(ResourceManager.class);
            URL url = rr.resolveResource(wsdlLocation, URL.class);
            
            if (url == null) {
                URIResolver res = new URIResolver(wsdlLocation);
                
                if (!res.isResolved()) {
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
                
                if (we != null && we.name().equals(port)){
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
        
        proxy.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
            uri.getAddress());

        client = ClientProxy.getClient(proxy);
        method = findMethod(clientCls);
    }

    protected void createClientFromLocalServer(final Bus bus) throws Exception, IOException 
    {
        String uri = getEndpoint().getEndpointURI().toString();
        int idx = uri.indexOf('?');
        if (idx != -1) {
            uri = uri.substring(0, idx);
        }
        
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);
        
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm.getDestinationFactoryForUri(uri);
        if (df == null) {
            throw new Exception("Could not find a destination factory for uri " + uri);
        }
        
        Destination dest = df.getDestination(ei);            
        try
        {
            MessageObserver mo = dest.getMessageObserver();
            if (mo instanceof ChainInitiationObserver) {
                ChainInitiationObserver cMo = (ChainInitiationObserver) mo;
                Endpoint cxfEP = cMo.getEndpoint();
                
                client = new ClientImpl(bus, cxfEP);
                client.getInInterceptors().add(new MuleHeadersInInterceptor());
                client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
                client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
                client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
            } else {
                throw new Exception("Could not create client! No Server was found directly on the endpoint: " + uri);
            }
        } catch (Exception ex) {
            disconnect();
            throw ex;
        }
    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispose()
    {
        // nothing to do
    }

    protected String getMethodName(UMOEvent event) throws DispatchException
    {
        // @TODO: Which of these *really* matter?
        String method = (String)event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);     

        if (method == null)
        {
            method = (String)event.getMessage().getProperty(CxfConstants.OPERATION);
        }

        if (method == null)
        {
            method = methodName;
        }
        
        if (method == null)
        {
            throw new DispatchException(SoapMessages.cannotInvokeCallWithoutOperation(), 
                event.getMessage(), event.getEndpoint());
        }
                
        return method;
    }

    protected Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.transformMessage();
        Object[] args;

        if (payload instanceof Object[])
        {
            args = (Object[])payload;
        }
        else
        {
            args = new Object[]{payload};
        }

        UMOMessage message = event.getMessage();
        Set attachmentNames = message.getAttachmentNames();
        if (attachmentNames != null && !attachmentNames.isEmpty())
        {
            List<DataHandler> attachments = new ArrayList<DataHandler>();
            for (Iterator i = attachmentNames.iterator(); i.hasNext();)
            {
                attachments.add(message.getAttachment((String)i.next()));
            }
            List<Object> temp = new ArrayList<Object>(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }

        return args;
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        ((ClientImpl)client).setSynchronousTimeout(event.getTimeout());
        if (proxy == null)
        {
            return doSendWithClient(event);
        }
        else
        {
            return doSendWithProxy(event);
        }
    }

    protected UMOMessage doSendWithProxy(UMOEvent event) throws Exception
    {
        Method localMethod = method;
        if (localMethod == null) 
        {
            localMethod = getMethodFromOperation((String)event.getMessage().getProperty(CxfConstants.OPERATION));
            
            if (localMethod == null)
            {
                String op = (String) endpoint.getProperty(CxfConstants.OPERATION);
                localMethod = getMethodFromOperation(op);
            }
        }
        
        if (localMethod == null)
        {
            throw new DispatchException(CxfMessages.noOperationWasFoundOrSpecified(), event.getMessage(), endpoint);
        }
        Object response = localMethod.invoke(proxy, getArgs(event));
        
        // TODO: handle holders
        
        return buildResponseMessage(event, new Object[] { response });
    }

    protected UMOMessage doSendWithClient(UMOEvent event) throws Exception
    {
        String method = getMethodName(event);

        // Set custom soap action if set on the event or endpoint
//        String soapAction = (String)event.getMessage().getProperty(SoapConstants.SOAP_ACTION_PROPERTY);
//        if (soapAction != null)
//        {
//            soapAction = parseSoapAction(soapAction, new QName(method), event);
//            this.client.setProperty(org.codehaus.xfire.soap.SoapConstants.SOAP_ACTION, soapAction);
//        }

        Map<String, Object> exProps = new HashMap<String, Object>();
        exProps.put(MuleProperties.MULE_EVENT_PROPERTY, event); 
        
        // Set Custom Headers on the client
        Object[] arr = event.getMessage().getPropertyNames().toArray();
        String head;

        for (int i = 0; i < arr.length; i++)
        {
            head = (String) arr[i];
            if ((head != null) && (!head.startsWith("MULE")))
            {
                exProps.put((String) arr[i], event.getMessage().getProperty((String) arr[i]));
            }
        }
        
        BindingOperationInfo bop = getOperation(method);
        
        Object[] response = client.invoke(bop, getArgs(event), exProps);

        return buildResponseMessage(event, response);
    }

    protected UMOMessage buildResponseMessage(UMOEvent event, Object[] response) 
    {
        UMOMessage result = null;
        if (response != null && response.length <= 1)
        {
            if (response.length == 1)
            {
                result = new MuleMessage(response[0], event.getMessage());
            }
        }
        else
        {
            result = new MuleMessage(response, event.getMessage());
        }

        return result;
    }

    protected BindingOperationInfo getOperation(String opName) throws Exception 
    {
        // Normally its not this hard to invoke the CXF Client, but we're 
        // sending along some exchange properties, so we need to use a more advanced method
        Endpoint ep = client.getEndpoint();
        QName q = new QName(ep.getService().getName().getNamespaceURI(), opName);
        BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
        if (bop == null) 
        {
            throw new Exception("No such operation: " + method);
        }
        
        if (bop.isUnwrappedCapable()) 
        {
            bop = bop.getUnwrappedOperation();
        }
        return bop;
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        doSend(event);
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        ((ClientImpl)client).setSynchronousTimeout((int)timeout);

        String method = (String)endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);

        if (method == null) 
        {
            method = (String)endpoint.getProperty(CxfConstants.OPERATION);
        }
        
        Properties params = endpoint.getEndpointURI().getUserParams();
        Object args[] = new Object[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        Object[] response = client.invoke(method, args);

        if (response != null && response.length == 1)
        {
            return new MuleMessage(response[0]);
        }
        else
        {
            return new MuleMessage(response);
        }
    }

}
