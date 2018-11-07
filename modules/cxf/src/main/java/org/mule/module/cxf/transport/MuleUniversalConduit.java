/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.transport;

import static org.mule.module.cxf.CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR;
import static org.mule.module.cxf.CxfConstants.MULE_EVENT;
import static org.mule.module.cxf.support.CxfUtils.clearClientContextIfNeeded;
import static org.mule.module.cxf.support.CxfUtils.resolveEncoding;
import static org.mule.transformer.types.DataTypeFactory.XML_STRING;
import static org.mule.transformer.types.DataTypeFactory.createFromParameterType;
import static org.mule.transport.http.HttpConnector.HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK;
import static org.mule.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static java.lang.Boolean.TRUE;
import static java.net.HttpURLConnection.HTTP_OK;
import static javax.xml.ws.BindingProvider.PASSWORD_PROPERTY;
import static javax.xml.ws.BindingProvider.USERNAME_PROPERTY;
import static org.apache.cxf.message.Message.ASYNC_POST_RESPONSE_DISPATCH;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;
import static org.apache.cxf.message.Message.ENCODING;
import static org.apache.cxf.message.Message.ENDPOINT_ADDRESS;
import static org.apache.cxf.message.Message.PATH_INFO;
import static org.apache.cxf.message.Message.QUERY_STRING;
import static org.apache.cxf.message.Message.RESPONSE_CODE;
import static org.apache.cxf.phase.Phase.PRE_STREAM;
import static org.mule.transport.http.HttpConstants.SC_ACCEPTED;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.cxf.CxfConfiguration;
import org.mule.module.cxf.CxfConstants;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.support.DelegatingOutputStream;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpClientMessageDispatcher;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * A Conduit is primarily responsible for sending messages from CXF to somewhere
 * else. This conduit takes messages which are being written and sends them to the
 * Mule bus.
 */
public class MuleUniversalConduit extends AbstractConduit
{

    private static final Logger LOGGER = LogUtils.getL7dLogger(MuleUniversalConduit.class);

    public static final String HTTP_STATUS = "http.status";

    private EndpointInfo endpoint;

    private CxfConfiguration configuration;

    private MuleUniversalTransport transport;

    private boolean closeInput = true;

    private Map<String,OutboundEndpoint> endpoints = new HashMap<String, OutboundEndpoint>();
    
    /**
     * @param ei The Endpoint being invoked by this destination.
     * @param t The EPR associated with this Conduit - i.e. the reply destination.
     */
    public MuleUniversalConduit(MuleUniversalTransport transport,
                                CxfConfiguration configuration,
                                EndpointInfo ei,
                                EndpointReferenceType t)
    {
        super(getTargetReference(ei, t));
        this.transport = transport;
        this.endpoint = ei;
        this.configuration = configuration;
    }

    @Override
    public void close(Message msg) throws IOException
    {
        OutputStream os = msg.getContent(OutputStream.class);
        if (os != null)
        {
            os.close();
        }
        
        if (closeInput)
        {
            InputStream in = msg.getContent(InputStream.class);
            if (in != null)
            {
                in.close();
            }
        }
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Prepare the message for writing.
     */
    public void prepare(final Message message) throws IOException
    {
        // save in a separate place in case we need to resend the request
        final ByteArrayOutputStream cache = new ByteArrayOutputStream();
        final DelegatingOutputStream delegating = new DelegatingOutputStream(cache);
        message.setContent(OutputStream.class, delegating);
        message.setContent(DelegatingOutputStream.class, delegating);
        
        OutputHandler handler = new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                synchronized (delegating.getOutputStream())
                {
                    out.write(cache.toByteArray());

                    delegating.setOutputStream(out);
                }

                // resume writing!
                message.getInterceptorChain().doIntercept(message);
            }
        };

        MuleEvent event = (MuleEvent) message.getExchange().get(MULE_EVENT);
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
            }
            catch (Exception e)
            {
                throw new Fault(e);
            }
        }
        else 
        {
            event.getMessage().setPayload(handler, XML_STRING);
        }

        if (!decoupled)
        {
            message.getExchange().put(MULE_EVENT, event);
        }
        message.put(MULE_EVENT, event);
        
        final MuleEvent finalEvent = event;
        final OutboundEndpoint finalEndpoint = ep;
        AbstractPhaseInterceptor<Message> i = new AbstractPhaseInterceptor<Message>(PRE_STREAM)
        {
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

    protected synchronized OutboundEndpoint getEndpoint(MuleContext muleContext, String uri) throws MuleException
    {
        if (endpoints.get(uri) != null)
        {
            return endpoints.get(uri);
        }

        OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(uri);
        endpoints.put(uri, endpoint);
        return endpoint;
    }
    
    public String setupURL(Message message) throws MalformedURLException
    {
        String value = (String) message.get(ENDPOINT_ADDRESS);
        String pathInfo = (String) message.get(PATH_INFO);
        String queryString = (String) message.get(QUERY_STRING);
        String username = (String) message.get(USERNAME_PROPERTY);
        String password = (String) message.get(PASSWORD_PROPERTY);

        String result = value != null ? value : getTargetOrEndpoint();

        if (username != null) {
             int slashIdx = result.indexOf("//");
             if (slashIdx != -1) {
                 result = result.substring(0, slashIdx + 2) + username + ":" + password + "@" + result.substring(slashIdx+2);
             }
        }

        // REVISIT: is this really correct?
        if (null != pathInfo && !result.endsWith(pathInfo))
        {
            result = result + pathInfo;
        }
        if (queryString != null)
        {
            result = result + "?" + queryString;
        }
        return result;
    }
    
    protected void dispatchMuleMessage(final Message m, MuleEvent reqEvent, OutboundEndpoint endpoint) throws MuleException {
        try
        {
            MuleMessage req = reqEvent.getMessage();
            req.setOutboundProperty(HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK, Boolean.TRUE.toString());

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
                        catch (IOException | HttpResponseException e)
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
        catch(MuleException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(MessageFactory.createStaticMessage("Could not send message to Mule."), e);
        }
    }

    private void sendResultBackToCxf(Message m, MuleEvent resEvent) throws TransformerException, IOException, HttpResponseException
    {
        if (resEvent != null && !VoidMuleEvent.getInstance().equals(resEvent))
        {
            m.getExchange().put(CxfConstants.MULE_EVENT, resEvent);

            // If we have a result, send it back to CXF
            MuleMessage result = resEvent.getMessage();
            InputStream is = getResponseBody(m, result);
            if (is != null)
            {
                Message inMessage = new MessageImpl();
                
                String contentType = result.getInboundProperty(HEADER_CONTENT_TYPE, "text/xml");

                String encoding = resolveEncoding(result, contentType);

                if (encoding != null)
                {
                    contentType += "; charset=" + encoding;
                }

                inMessage.put(ENCODING, encoding);
                inMessage.put(CONTENT_TYPE, contentType);
                inMessage.setContent(InputStream.class, is);
                inMessage.setExchange(m.getExchange());
                getMessageObserver().onMessage(inMessage);
                clearClientContextIfNeeded(getMessageObserver());
                return;
            }
            /*
                If this branch is reached, it means that the message payload is empty. By asserting that a ClientCallback is defined in the message exchange, I'm certain that
                the message processing is being executed in a non-blocking fashion. So, the only way it is going to be replied, is through that callback. To avoid the flow
                from finishing execution silently, an exception is thrown with the SOAP service response status code.
            */
            else if (m.getExchange().get(ClientCallback.class) != null && resEvent.getMessage().getInboundPropertyNames().contains(HTTP_STATUS))
            {
                int httpResponseStatusCode = Integer.valueOf(resEvent.getMessage().getInboundProperty(HTTP_STATUS).toString());
                throw new HttpResponseException("HTTP Response payload empty can't be processed through CXF components", httpResponseStatusCode);
            }
        }

        // No body in the response, mark the exchange as finished.
        m.getExchange().put(ClientImpl.FINISHED, Boolean.TRUE);
    }

    protected InputStream getResponseBody(Message m, MuleMessage result) throws TransformerException, IOException
    {
        boolean response = result != null
                           && !NullPayload.getInstance().equals(result.getPayload())
                           && !isOneway(m.getExchange());

        if (response)
        {
            // Sometimes there may not actually be a body, in which case
            // we want to act appropriately. E.g. one way invocations over a proxy
            InputStream is = result.getPayload(DataTypeFactory.create(InputStream.class));
            PushbackInputStream pb = new PushbackInputStream(is);
            result.setPayload(pb, XML_STRING);

            int b = pb.read();
            if (b != -1)
            {
                pb.unread(b);
                return pb;
            }
        }

        return null;
    }

    protected boolean isOneway(Exchange exchange)
    {
        return exchange != null && exchange.isOneWay();
    }
    
    protected String getTargetOrEndpoint()
    {
        if (target != null)
        {
            return target.getAddress().getValue();
        }

        return endpoint.getAddress();
    }

    public void onClose(final Message m) throws IOException
    {
        // template method
    }
    
    protected MuleEvent processNext(MuleEvent event,
                                    Exchange exchange, OutboundEndpoint endpoint) throws MuleException
    {
        CxfOutboundMessageProcessor processor = (CxfOutboundMessageProcessor) exchange.get(CXF_OUTBOUND_MESSAGE_PROCESSOR);
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
        
        return response;
    }

    @Override
    public void close()
    {
    }

    /**
     * Get the target endpoint reference.
     * 
     * @param ei the corresponding EndpointInfo
     * @param t the given target EPR if available
     * @return the actual target
     */
    protected static EndpointReferenceType getTargetReference(EndpointInfo ei, EndpointReferenceType t)
    {
        EndpointReferenceType ref = null;
        if (null == t)
        {
            ref = new EndpointReferenceType();
            AttributedURIType address = new AttributedURIType();
            address.setValue(ei.getAddress());
            ref.setAddress(address);
            if (ei.getService() != null)
            {
                EndpointReferenceUtils.setServiceAndPortName(ref, ei.getService().getName(), ei.getName()
                    .getLocalPart());
            }
        }
        else
        {
            ref = t;
        }
        return ref;
    }

    /**
     * Used to set appropriate message properties, exchange etc. as required for an
     * incoming decoupled response (as opposed what's normally set by the Destination
     * for an incoming request).
     */
    protected class InterposedMessageObserver implements MessageObserver
    {
        /**
         * Called for an incoming message.
         * 
         * @param inMessage
         */
        public void onMessage(Message inMessage)
        {
            // disposable exchange, swapped with real Exchange on correlation
            inMessage.setExchange(new ExchangeImpl());
            inMessage.put(DECOUPLED_CHANNEL_MESSAGE, TRUE);
            inMessage.put(RESPONSE_CODE, HTTP_OK);
            inMessage.remove(ASYNC_POST_RESPONSE_DISPATCH);

            incomingObserver.onMessage(inMessage);
            clearClientContextIfNeeded(getMessageObserver());
        }
    }
    
    public void setCloseInput(boolean closeInput)
    {
        this.closeInput = closeInput;
    }

    protected CxfConfiguration getConnector()
    {
        return configuration;
    }

    protected EndpointInfo getEndpoint()
    {
        return endpoint;
    }

    protected MuleUniversalTransport getTransport()
    {
        return transport;
    }
}
