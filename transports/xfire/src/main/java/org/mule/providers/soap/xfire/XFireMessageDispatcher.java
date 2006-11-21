/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.handler.Handler;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.FatalConnectException;
import org.mule.providers.soap.SoapConstants;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.TemplateParser;

/**
 * The XFireMessageDispatcher is used for making Soap client requests to remote
 * services.
 */
public class XFireMessageDispatcher extends AbstractMessageDispatcher
{
    protected final XFireConnector connector;
    protected volatile ObjectPool clientPool;
    private final TemplateParser soapActionTemplateParser = TemplateParser.createAntStyleParser();

    public XFireMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XFireConnector)endpoint.getConnector();
    }

    protected void doConnect(final UMOImmutableEndpoint endpoint) throws Exception
    {
        if (clientPool == null)
        {
            final XFire xfire = connector.getXfire();
            final String serviceName = getServiceName(endpoint);
            final Service service = xfire.getServiceRegistry().getService(serviceName);

            if (service == null)
            {
                throw new FatalConnectException(new Message("xfire", 8, serviceName), this);
            }

            // Add Security Handlers
            if (connector.getWsSecurity() != null)
            {

                if (connector.getWsSecurity().get("serviceInHandlers") != null)
                {
                    List inList = (List)connector.getWsSecurity().get("serviceInHandlers");
                    for (int i = 0; i < inList.size(); i++)
                    {
                        Class clazz = Class.forName(inList.get(i).toString());
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addInHandler(handler);
                    }

                    if (!inList.contains("org.codehaus.xfire.security.wss4j.WSS4JInHandler"))
                    {
                        Class clazz = Class.forName("org.codehaus.xfire.security.wss4j.WSS4JInHandler");
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addInHandler(handler);
                    }

                    if (!inList.contains("org.codehaus.xfire.util.dom.DOMInHandler"))
                    {
                        Class clazz = Class.forName("org.codehaus.xfire.util.dom.DOMInHandler");
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addInHandler(handler);
                    }
                }

                if (connector.getWsSecurity().get("serviceOutHandlers") != null)
                {
                    List outList = (List)connector.getWsSecurity().get("serviceOutHandlers");
                    for (int i = 0; i < outList.size(); i++)
                    {
                        Class clazz = Class.forName(outList.get(i).toString());
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addOutHandler(handler);
                    }

                    if (!outList.contains("org.codehaus.xfire.security.wss4j.WSS4JOutHandler"))
                    {
                        Class clazz = Class.forName("org.codehaus.xfire.security.wss4j.WSS4JOutHandler");
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addOutHandler(handler);
                    }

                    if (!outList.contains("org.codehaus.xfire.util.dom.DOMOutHandler"))
                    {
                        Class clazz = Class.forName("org.codehaus.xfire.util.dom.DOMOutHandler");
                        Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                        service.addOutHandler(handler);
                    }
                }
            }
            try
            {
                clientPool = new StackObjectPool(new XFireClientPoolFactory(endpoint, service, xfire));
                clientPool.addObject();
            }
            catch (Exception ex)
            {
                disconnect();
                throw ex;
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (clientPool != null)
        {
            clientPool.clear();
            clientPool = null;
        }
    }

    protected void doDispose()
    {
        // nothing to do
    }

    protected String getMethod(UMOEvent event) throws DispatchException
    {
        String method = (String)event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        if (method == null)
        {
            UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
            method = (String)endpointUri.getParams().get(MuleProperties.MULE_METHOD_PROPERTY);
        }

        if (method == null)
        {
            method = (String)event.getEndpoint().getProperties().get(MuleProperties.MULE_METHOD_PROPERTY);
            if (method == null)
            {
                throw new DispatchException(new org.mule.config.i18n.Message("soap", 4), event.getMessage(),
                    event.getEndpoint());
            }
        }
        return method;
    }

    protected Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
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
            List attachments = new ArrayList();
            for (Iterator i = attachmentNames.iterator(); i.hasNext();)
            {
                attachments.add(message.getAttachment((String)i.next()));
            }
            List temp = new ArrayList(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }

        return args;
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        Client client = null;

        try
        {
            client = (Client)clientPool.borrowObject();
            client.setTimeout(event.getTimeout());
            client.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
            String method = getMethod(event);
            // Set custom soap action if set on the event or endpoint
            String soapAction = (String)event.getMessage().getProperty(SoapConstants.SOAP_ACTION_PROPERTY);
            if (soapAction != null)
            {
                soapAction = parseSoapAction(soapAction, new QName(method), event);
                client.setProperty(org.codehaus.xfire.soap.SoapConstants.SOAP_ACTION, soapAction);
            }

            // Ws Security
            if (event.getMessage().getProperty(WSHandlerConstants.ACTION) != null)
            {
                client.setProperty(WSHandlerConstants.ACTION, event.getMessage().getProperty(
                    WSHandlerConstants.ACTION));

                String passwordType;
                if (event.getMessage().getProperty(WSConstants.PW_DIGEST) != null)
                {
                    passwordType = WSConstants.PW_DIGEST;
                }
                else
                {
                    passwordType = WSConstants.PW_TEXT;
                }
                client.setProperty(WSHandlerConstants.PASSWORD_TYPE, passwordType);
                client.setProperty(WSHandlerConstants.USER, event.getMessage().getProperty(
                    WSHandlerConstants.USER));
                client.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, event.getMessage().getProperty(
                    WSHandlerConstants.PW_CALLBACK_CLASS));

                // if encrypted
                if (event.getMessage().getProperty(WSHandlerConstants.ENC_PROP_FILE) != null)
                {
                    client.setProperty(WSHandlerConstants.ENC_PROP_FILE, event.getMessage().getProperty(
                        WSHandlerConstants.ENC_PROP_FILE));
                }
            }

            Object[] response = client.invoke(method, getArgs(event));

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
        finally
        {
            if (client != null)
            {
                clientPool.returnObject(client);
            }
        }
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        Client client = null;

        try
        {
            client = (Client)clientPool.borrowObject();
            client.setTimeout(event.getTimeout());
            client.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
            client.invoke(getMethod(event), getArgs(event));
        }
        finally
        {
            if (client != null)
            {
                clientPool.returnObject(client);
            }
        }
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        String serviceName = getServiceName(endpoint);

        XFire xfire = connector.getXfire();
        Service service = xfire.getServiceRegistry().getService(serviceName);

        Client client = new Client(new MuleUniversalTransport(), service, endpoint.getEndpointURI()
            .toString());
        client.setXFire(xfire);
        client.setTimeout((int)timeout);
        client.setEndpointUri(endpoint.getEndpointURI().toString());

        String method = (String)endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        OperationInfo op = service.getServiceInfo().getOperation(method);

        Properties params = endpoint.getEndpointURI().getUserParams();
        String args[] = new String[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        Object[] response = client.invoke(op, args);

        if (response != null && response.length == 1)
        {
            return new MuleMessage(response[0]);
        }
        else
        {
            return new MuleMessage(response);
        }
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /**
     * Get the service that is mapped to the specified request.
     */
    protected String getServiceName(UMOImmutableEndpoint endpoint)
    {
        String pathInfo = endpoint.getEndpointURI().getPath();

        if (StringUtils.isEmpty(pathInfo))
        {
            return endpoint.getEndpointURI().getHost();
        }

        String serviceName;

        int i = pathInfo.lastIndexOf('/');

        if (i > -1)
        {
            serviceName = pathInfo.substring(i + 1);
        }
        else
        {
            serviceName = pathInfo;
        }

        return serviceName;
    }

    public String parseSoapAction(String soapAction, QName method, UMOEvent event)
    {

        UMOEndpointURI endpointURI = event.getEndpoint().getEndpointURI();
        Map properties = new HashMap();
        UMOMessage msg = event.getMessage();
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            properties.put(propertyKey, msg.getProperty(propertyKey));
        }
        properties.put(MuleProperties.MULE_METHOD_PROPERTY, method.getLocalPart());
        properties.put("methodNamespace", method.getNamespaceURI());
        properties.put("address", endpointURI.getAddress());
        properties.put("scheme", endpointURI.getScheme());
        properties.put("host", endpointURI.getHost());
        properties.put("port", String.valueOf(endpointURI.getPort()));
        properties.put("path", endpointURI.getPath());
        properties.put("hostInfo", endpointURI.getScheme()
                                   + "://"
                                   + endpointURI.getHost()
                                   + (endpointURI.getPort() > -1
                                                   ? ":" + String.valueOf(endpointURI.getPort()) : ""));
        if (event.getComponent() != null)
        {
            properties.put("serviceName", event.getComponent().getDescriptor().getName());
        }

        soapAction = soapActionTemplateParser.parse(properties, soapAction);

        if (logger.isDebugEnabled())
        {
            logger.debug("SoapAction for this call is: " + soapAction);
        }

        return soapAction;
    }
}
