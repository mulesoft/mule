/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.soap.axis.extensions.MuleSoapHeadersHandler;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
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
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class AxisMessageDispatcher extends AbstractMessageDispatcher
{
//    private Call call;
//    private Object callSemaphore = new Object();

    public AxisMessageDispatcher(AxisConnector connector)
    {
        super(connector);
        //service = new Service(connector.getClientProvider());
    }

    public void doDispose() throws UMOException
    {
    }

    public synchronized void doDispatch(UMOEvent event) throws Exception
    {
        Call call = getCall(event);
        call.invokeOneWay(getArgs(event));
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

    private Call getCall(UMOEvent event) throws ServiceException, TransformerException, DispatchException, MalformedEndpointException
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        String method = (String) endpointUri.getParams().remove("method");

        if(method==null) {
            method = (String)event.getEndpoint().getProperties().get("method");
            if(method==null) {
                throw new DispatchException("Cannot invoke axis call without an Operation. Set the method param on your axis endpointUri");
            }
        }

        //synchronized(callSemaphore) {
        //if(call==null) {
            Call call = (Call) new Service().createCall();
            call.setTargetEndpointAddress(endpointUri.getAddress());
            call.setSOAPActionURI(endpointUri.getAddress());
            call.setClientHandlers(new MuleSoapHeadersHandler(), new MuleSoapHeadersHandler());
//       } else {
//            call.clearOperation();
       //}
        call.setOperationName(method);
        //set Mule event here so that hsandlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        //}
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

    private void setMessageContextProperties(UMOMessage message, MessageContext ctx) {
        Object temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if(temp!=null && !"".equals(temp.toString())) {
            message.setCorrelationId(temp.toString());
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if(temp!=null && !"".equals(temp.toString())) {
            message.setCorrelationGroupSize(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if(temp!=null && !"".equals(temp.toString())) {
            message.setCorrelationSequence(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if(temp!=null && !"".equals(temp.toString())) {
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
        for (Iterator iterator = params.values().iterator(); iterator.hasNext();i++)
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

    protected UMOMessage createMessage(Object result, Call call) {
        if (result == null)
        {
            result = new NullPayload();
        }
        Map props = new HashMap();
        Iterator iter = call.getMessageContext().getPropertyNames();
        Object key;
        while(iter.hasNext()) {
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
