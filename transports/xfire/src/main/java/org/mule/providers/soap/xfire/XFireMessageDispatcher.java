/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.soap.SoapConstants;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.aegis.type.TypeMapping;
import org.codehaus.xfire.aegis.type.basic.BeanType;
import org.codehaus.xfire.client.Client;

/**
 * The XFireMessageDispatcher is used for making Soap client requests to remote
 * services.
 */
public class XFireMessageDispatcher extends AbstractMessageDispatcher
{
    // Since the MessageDispatcher is guaranteed to serve a single thread,
    // the Dispatcher can own the xfire Client as an instance variable
    protected Client client = null;
    protected final XFireConnector connector;
    private final TemplateParser soapActionTemplateParser = TemplateParser.createAntStyleParser();

    public XFireMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XFireConnector) endpoint.getConnector();
    }
    
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = connector.doClientConnect(endpoint, this);
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
        if (event.getEndpoint().getProperty("complexTypes") != null)
        {
            configureClientForComplexTypes(this.client, event);
        }
        this.client.setTimeout(event.getTimeout());
        this.client.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        String method = getMethod(event);

        // Set custom soap action if set on the event or endpoint
        String soapAction = (String)event.getMessage().getProperty(SoapConstants.SOAP_ACTION_PROPERTY);
        if (soapAction != null)
        {
            soapAction = parseSoapAction(soapAction, new QName(method), event);
            this.client.setProperty(org.codehaus.xfire.soap.SoapConstants.SOAP_ACTION, soapAction);
        }

        // Set Custom Headers on the client
        Object[] arr = event.getMessage().getPropertyNames().toArray();
        String head;

        for (int i = 0; i < arr.length; i++)
        {
            head = (String)arr[i];
            if ((head != null)&&(!head.startsWith("MULE"))){
                this.client.setProperty((String)arr[i], event.getMessage().getProperty((String)arr[i]));
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

    protected void doDispatch(UMOEvent event) throws Exception
    {
        this.client.setTimeout(event.getTimeout());
        this.client.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        this.client.invoke(getMethod(event), getArgs(event));
    }

    /**
     * Get the service that is mapped to the specified request.
     */
    protected static String getServiceName(UMOImmutableEndpoint endpoint)
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
            properties.put("serviceName", event.getComponent().getName());
        }

        soapAction = soapActionTemplateParser.parse(properties, soapAction);

        if (logger.isDebugEnabled())
        {
            logger.debug("SoapAction for this call is: " + soapAction);
        }

        return soapAction;
    }

    protected void configureClientForComplexTypes(Client client, UMOEvent event) throws ClassNotFoundException
    {
        Map complexTypes = (Map) event.getEndpoint().getProperty("complexTypes");
        Object[] beans = complexTypes.keySet().toArray();

        AegisBindingProvider bp = (AegisBindingProvider) client.getService().getBindingProvider();
        TypeMapping typeMapping = bp.getTypeMapping(client.getService());

        // for each complex type
        for (int i = 0; i < beans.length; i++)
        {
            BeanType bt = new BeanType();
            String[] queue = ((String) complexTypes.get(beans[i])).split(":", 2);
            bt.setSchemaType(new QName(queue[1], queue[0]));
            bt.setTypeClass(Class.forName(beans[i].toString()));
            typeMapping.register(bt);
        }
    }
}
