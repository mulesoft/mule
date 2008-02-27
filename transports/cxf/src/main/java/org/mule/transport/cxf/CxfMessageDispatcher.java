/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.AbstractMessageDispatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.DataHandler;

import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * The CxfMessageDispatcher is used for making Soap client requests to remote
 * services.
 */
public class CxfMessageDispatcher extends AbstractMessageDispatcher
{

    private static final String URI_REGEX = "cxf:\\[(.+?)\\]:(.+?)/\\[(.+?)\\]:(.+?)";
    Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    protected final CxfConnector connector;
    protected ClientWrapper wrapper;
    
    public CxfMessageDispatcher(OutboundEndpoint endpoint)
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
        wrapper = new ClientWrapper();
        wrapper.setBus(connector.getCxfBus());
        wrapper.setEndpoint(endpoint);
        wrapper.initialize();
    }

    protected void doDisconnect() throws Exception
    {
        wrapper = null;
    }

    protected void doDispose()
    {
        // nothing to do
    }


    protected Object[] getArgs(MuleEvent event) throws TransformerException
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

        MuleMessage message = event.getMessage();
        Set<?> attachmentNames = message.getAttachmentNames();
        if (attachmentNames != null && !attachmentNames.isEmpty())
        {
            List<DataHandler> attachments = new ArrayList<DataHandler>();
            for (Iterator<?> i = attachmentNames.iterator(); i.hasNext();)
            {
                attachments.add(message.getAttachment((String)i.next()));
            }
            List<Object> temp = new ArrayList<Object>(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }

        return args;
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        ((ClientImpl)wrapper.getClient()).setSynchronousTimeout(event.getTimeout());
        if (!wrapper.isProxy())
        {
            return doSendWithClient(event);
        }
        else
        {
            return doSendWithProxy(event);
        }
    }

    protected MuleMessage doSendWithProxy(MuleEvent event) throws Exception
    {
        Method method = wrapper.getMethod(event);
        Object response = method.invoke(wrapper.getProxy(), getArgs(event));
        
        // TODO: handle holders
        
        return buildResponseMessage(event, new Object[] { response });
    }

    protected MuleMessage doSendWithClient(MuleEvent event) throws Exception
    {
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
        
        BindingOperationInfo bop = wrapper.getOperation(event);
        
        Object[] response = wrapper.getClient().invoke(bop, getArgs(event), exProps);

        return buildResponseMessage(event, response);
    }

    protected MuleMessage buildResponseMessage(MuleEvent event, Object[] response) 
    {
        MuleMessage result = null;
        if (response != null && response.length <= 1)
        {
            if (response.length == 1)
            {
                result = new DefaultMuleMessage(response[0], event.getMessage());
            }
        }
        else
        {
            result = new DefaultMuleMessage(response, event.getMessage());
        }

        return result;
    }
    protected void doDispatch(MuleEvent event) throws Exception
    {
        doSend(event);
    }


}
