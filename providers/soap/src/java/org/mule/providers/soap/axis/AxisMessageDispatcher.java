/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis;

import org.apache.axis.*;
import org.apache.axis.client.AxisClient;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPTransport;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.soap.axis.extensions.MuleHttpSender;
import org.mule.providers.soap.axis.extensions.MuleSoapHeadersHandler;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;

import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPEnvelope;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the
 * Axis soap client.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisMessageDispatcher extends AbstractMessageDispatcher
{
    private Service service;

    public AxisMessageDispatcher(AxisConnector connector)
    {
        super(connector);
        try
        {
            service = new Service();
            //todo for some reason loading the client handlers this way isn't working for me?
            //Doesn't seem to load the optimised http transport...
            //SimpleProvider clientConfig = connector.getClientProvider();

            SimpleProvider clientConfig = new SimpleProvider();
            Handler muleHandler = (Handler) new MuleSoapHeadersHandler();
            SimpleChain reqHandler = new SimpleChain();
            SimpleChain respHandler = new SimpleChain();
            reqHandler.addHandler(muleHandler);
            respHandler.addHandler(muleHandler);
            Handler pivot = (Handler) new MuleHttpSender();
            Handler transport = new SimpleTargetedChain(reqHandler, pivot, respHandler);
            clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME, transport);

            service.setEngineConfiguration(clientConfig);
            service.setEngine(new AxisClient(clientConfig));
        } catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }

    public void doDispose()
    {
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        Call call = getCall(event);
        //dont use invokeOneWay here as we are already in a thread pool.
        //Axis creates a new thread for every invoke one way call. nasty!
        Object[] args = getArgs(event);
        call.setProperty("axis.one.way", Boolean.TRUE);
        call.invoke(args);

    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        Call call = getCall(event);
        Object result = call.invoke(getArgs(event));
        if (result == null)
        {
            return null;
        } else
        {
            UMOMessage resultMessage = new MuleMessage(result, event.getProperties());
            setMessageContextProperties(resultMessage, call.getMessageContext());
            return resultMessage;
        }
    }

    private Call getCall(UMOEvent event) throws ServiceException, DispatchException
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        String method = (String) endpointUri.getParams().remove("method");

        if (method == null)
        {
            method = (String) event.getEndpoint().getProperties().get("method");
            if (method == null)
            {
                throw new DispatchException(new org.mule.config.i18n.Message("soap", 4), event.getMessage(),  event.getEndpoint());
            }
        }
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(endpointUri.getAddress());
        call.setSOAPActionURI(endpointUri.getAddress());
        call.setOperationName(method);
        //set Mule event here so that hsandlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        return call;
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        Object[] args;
        if (payload instanceof Object[])
        {
            args = (Object[]) payload;
        } else
        {
            args = new Object[]{payload};
        }
        return args;
    }

    private void setMessageContextProperties(UMOMessage message, MessageContext ctx)
    {
        Object temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (temp != null && !"".equals(temp.toString()))
        {
            message.setCorrelationId(temp.toString());
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (temp != null && !"".equals(temp.toString()))
        {
            message.setCorrelationGroupSize(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (temp != null && !"".equals(temp.toString()))
        {
            message.setCorrelationSequence(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (temp != null && !"".equals(temp.toString()))
        {
            message.setReplyTo(temp.toString());
        }
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        String address = endpointUri.getAddress();
        Call call = new Call(address);
        call.setSOAPActionURI(address);
        call.setTargetEndpointAddress(address);

        String method = (String) endpointUri.getParams().remove("method");
        call.setOperationName(method);
        Properties params = endpointUri.getUserParams();
        Object args[] = new Object[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next();
        }

        call.setOperationName(method);
        Object result = call.invoke(method, args);
        return createMessage(result, call);
    }

    public UMOMessage receive(String endpoint, Object[] args) throws Exception
    {
        Call call = new Call(endpoint);
        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);

        if (!endpoint.startsWith("axis:"))
        {
            endpoint = "axis:" + endpoint;
        }
        UMOEndpointURI ep = new MuleEndpointURI(endpoint);
        String method = (String) ep.getParams().remove("method");
        call.setOperationName(method);

        call.setOperationName(method);
        Object result = call.invoke(method, args);
        return createMessage(result, call);
    }

    public UMOMessage receive(String endpoint, SOAPEnvelope envelope) throws Exception
    {
        Call call = new Call(endpoint);
        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);
        Object result = call.invoke(new Message(envelope));
        return createMessage(result, call);
    }

    protected UMOMessage createMessage(Object result, Call call)
    {
        if (result == null)
        {
            result = new NullPayload();
        }
        Map props = new HashMap();
        Iterator iter = call.getMessageContext().getPropertyNames();
        Object key;
        while (iter.hasNext())
        {
            key = iter.next();
            props.put(key, call.getMessageContext().getProperty(key.toString()));
        }
        props.put("soap.message", call.getMessageContext().getMessage());
        return new MuleMessage(result, props);
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }
}
