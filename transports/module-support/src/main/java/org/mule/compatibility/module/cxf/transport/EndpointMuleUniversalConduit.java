/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.transport;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.support.DelegatingOutputStream;
import org.mule.runtime.module.cxf.transport.MuleUniversalConduit;
import org.mule.runtime.module.cxf.transport.MuleUniversalTransport;
import org.mule.runtime.module.http.api.HttpConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Holder;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class EndpointMuleUniversalConduit extends MuleUniversalConduit
{

    private Map<String, OutboundEndpoint> endpoints = new HashMap<String, OutboundEndpoint>();

    public EndpointMuleUniversalConduit(MuleUniversalTransport transport, CxfConfiguration configuration, EndpointInfo ei, EndpointReferenceType t)
    {
        super(transport, configuration, ei, t);
    }

    protected synchronized OutboundEndpoint getEndpoint(MuleContext muleContext, String uri) throws MuleException
    {
        if (endpoints.get(uri) != null)
        {
            return endpoints.get(uri);
        }

        OutboundEndpoint endpoint = getEndpointFactory(muleContext).getOutboundEndpoint(uri);
        endpoints.put(uri, endpoint);
        return endpoint;
    }

    private static EndpointFactory getEndpointFactory(MuleContext muleContext)
    {
        return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }

    /**
     * Prepare the message for writing.
     */
    @Override
    public void prepare(final Message message) throws IOException
    {
        // save in a separate place in case we need to resend the request
        final ByteArrayOutputStream cache = new ByteArrayOutputStream();
        final DelegatingOutputStream delegating = new DelegatingOutputStream(cache);
        message.setContent(OutputStream.class, delegating);
        message.setContent(DelegatingOutputStream.class, delegating);

        OutputHandler handler = new OutputHandler()
        {
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                out.write(cache.toByteArray());

                delegating.setOutputStream(out);

                // resume writing!
                message.getInterceptorChain().doIntercept(message);
            }
        };

        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
        // are we sending an out of band response for a server side request?
        boolean decoupled = event != null && message.getExchange().getInMessage() != null;

        OutboundEndpoint ep = null;

        if (event == null || VoidMuleEvent.getInstance().equals(event) || decoupled)
        {
            // we've got an out of band WS-RM message or a message from a standalone client
            MuleContext muleContext = configuration.getMuleContext();
            MuleMessage muleMsg = new DefaultMuleMessage(handler, muleContext);

            String url = setupURL(message);

            try
            {
                ep = getEndpoint(muleContext, url);
                event = new DefaultMuleEvent(muleMsg, ep.getExchangePattern(), (FlowConstruct) null);
                // event = new DefaultMuleEvent(muleMsg, (FlowConstruct) null);
            }
            catch (Exception e)
            {
                throw new Fault(e);
            }
        }
        else
        {
            event.getMessage().setPayload(handler, DataTypeFactory.XML_STRING);
        }

        if (!decoupled)
        {
            message.getExchange().put(CxfConstants.MULE_EVENT, event);
        }
        message.put(CxfConstants.MULE_EVENT, event);

        final MuleEvent finalEvent = event;
        final OutboundEndpoint finalEndpoint = ep;
        AbstractPhaseInterceptor<Message> i = new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM)
        {
            @Override
            public void handleMessage(Message m) throws Fault
            {
                try
                {
                    dispatchMuleMessage(m, finalEvent, finalEndpoint);
                }
                catch (MuleException e)
                {
                    throw new Fault(e);
                }
            }
        };
        message.getInterceptorChain().add(i);
    }

    protected void dispatchMuleMessage(final Message m, MuleEvent reqEvent, OutboundEndpoint endpoint) throws MuleException
    {
        try
        {
            MuleMessage req = reqEvent.getMessage();
            req.setOutboundProperty(HttpConstants.RequestProperties.HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK, Boolean.TRUE.toString());

            if (reqEvent.isAllowNonBlocking())
            {
                final ReplyToHandler originalReplyToHandler = reqEvent.getReplyToHandler();

                reqEvent = new DefaultMuleEvent(reqEvent, new NonBlockingReplyToHandler()
                {
                    @Override
                    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
                    {
                        try
                        {
                            Holder<MuleEvent> holder = (Holder<MuleEvent>) m.getExchange().get("holder");
                            holder.value = event;
                            sendResultBackToCxf(m, event);
                        }
                        catch (IOException e)
                        {
                            processExceptionReplyTo(new MessagingException(event, e), replyTo);
                        }
                    }

                    @Override
                    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
                    {
                        originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
                    }
                });
            }
            // Update RequestContext ThreadLocal for backwards compatibility
            OptimizedRequestContext.unsafeSetEvent(reqEvent);

            MuleEvent resEvent = processNext(reqEvent, m.getExchange(), endpoint);

            if (!resEvent.equals(NonBlockingVoidMuleEvent.getInstance()))
            {
                sendResultBackToCxf(m, resEvent);
            }
        }
        catch (MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(MessageFactory.createStaticMessage("Could not send message to Mule."), e);
        }
    }

    protected MuleEvent processNext(MuleEvent event,
                                    Exchange exchange, OutboundEndpoint endpoint)
            throws MuleException
    {
        CxfOutboundMessageProcessor processor = (CxfOutboundMessageProcessor) exchange.get(CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR);
        MuleEvent response;
        if (processor == null)
        {
            response = endpoint.process(event);
        }
        else
        {
            response = processor.processNext(event);

            Holder<MuleEvent> holder = (Holder<MuleEvent>) exchange.get("holder");
            holder.value = response;
        }

        // response = processor.processNext(event);
        //
        // Holder<MuleEvent> holder = (Holder<MuleEvent>) exchange.get("holder");
        // holder.value = response;
        return response;
    }

}
