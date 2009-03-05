/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.transport;

import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;
import static org.mule.api.config.MuleProperties.MULE_EVENT_PROPERTY;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.MuleServer;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.NullPayload;
import org.mule.transport.cxf.CxfConnector;
import org.mule.transport.cxf.CxfConstants;
import org.mule.transport.cxf.support.DelegatingOutputStream;
import org.mule.transport.cxf.support.MuleProtocolHeadersOutInterceptor;
import org.mule.transport.http.HttpConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.Destination;
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

    private EndpointInfo endpoint;

    private CxfConnector connector;

    private Destination decoupledDestination;

    private String decoupledEndpoint;

    private MuleUniversalTransport transport;

    private int decoupledDestinationRefCount;

    private boolean closeInput;

    private boolean applyTransformersToProtocol;
    
    private ImmutableEndpoint muleEndpoint;

    private Map<String,OutboundEndpoint> protocolEndpoints = new HashMap<String, OutboundEndpoint>();
    
    /**
     * @param ei The Endpoint being invoked by this destination.
     * @param t The EPR associated with this Conduit - i.e. the reply destination.
     */
    public MuleUniversalConduit(MuleUniversalTransport transport,
                                CxfConnector connector,
                                EndpointInfo ei,
                                EndpointReferenceType t)
    {
        super(getTargetReference(ei, t));
        this.transport = transport;
        this.endpoint = ei;
        this.connector = connector;
    }
    
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

    public synchronized Destination getBackChannel()
    {
        if (decoupledDestination == null && decoupledEndpoint != null)
        {
            setUpDecoupledDestination();
        }
        return decoupledDestination;
    }

    protected void setUpDecoupledDestination()
    {
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(decoupledEndpoint);
        try
        {
            decoupledDestination = transport.getDestination(ei);
            decoupledDestination.setMessageObserver(new InterposedMessageObserver());
            duplicateDecoupledDestination();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
        
        AbstractPhaseInterceptor<Message> i = new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM)
        {
            public void handleMessage(Message m) throws Fault
            {
                try
                {
                    dispatchMuleMessage(m);
                }
                catch (IOException e)
                {
                    throw new Fault(e);
                }
            }
        };
        i.getAfter().add(MuleProtocolHeadersOutInterceptor.class.getName());
        message.getInterceptorChain().add(i);
        
        OutputHandler handler = new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                out.write(cache.toByteArray());
                
                delegating.setOutputStream(out);
                
                // resume writing!
                message.getInterceptorChain().doIntercept(message);
            }
        };

        MuleEvent event = (MuleEvent) message.getExchange().get(MULE_EVENT_PROPERTY);
        
        DefaultMessageAdapter req;
        if (event == null) 
        {
            req = new DefaultMessageAdapter(handler);
        }
        else 
        {
            req = new DefaultMessageAdapter(handler, event.getMessage());
        }
        
        message.getExchange().put(CxfConstants.MULE_MESSAGE, req);
    }
    
    protected void dispatchMuleMessage(Message m) throws IOException {
        String uri = setupURL(m);

        LOGGER.info("Sending message to " + uri);
        try
        {
            OutboundEndpoint protocolEndpoint = getProtocolEndpoint(uri);

            MessageAdapter req = (MessageAdapter) m.getExchange().get(CxfConstants.MULE_MESSAGE);
            req.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, uri, PropertyScope.INVOCATION);
            
            MuleMessage result = sendStream(req, protocolEndpoint, m.getExchange());

            if (result == null)
            {
                m.getExchange().put(ClientImpl.FINISHED, Boolean.TRUE);
                return;
            }
            
            // If we have a result, send it back to CXF
            InputStream is = getResponseBody(m, result);
            if (is != null)
            {
                Message inMessage = new MessageImpl();

                String contentType = result.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
                inMessage.put(Message.CONTENT_TYPE, contentType);
                inMessage.put(Message.ENCODING, result.getEncoding());
                inMessage.put(CxfConstants.MULE_MESSAGE, result);
                inMessage.setContent(InputStream.class, is);
                inMessage.setExchange(m.getExchange());
                getMessageObserver().onMessage(inMessage);
            }
        }
        catch (Exception e)
        {
            if (e instanceof IOException)
            {
                throw (IOException) e;
            }

            IOException ex = new IOException("Could not send message to Mule.");
            ex.initCause(e);
            throw ex;
        }
    }

    protected OutboundEndpoint getProtocolEndpoint(String uri) throws MuleException
    {
        OutboundEndpoint ep = protocolEndpoints.get(uri);
        if (ep == null)
        {
            ep = initializeProtocolEndpoint(uri);
        }
        return ep;
    }

    protected synchronized OutboundEndpoint initializeProtocolEndpoint(String uri) throws MuleException
    {
        OutboundEndpoint ep = protocolEndpoints.get(uri);
        if (ep != null) return ep;
        
        MuleContext muleContext = MuleServer.getMuleContext();
        MuleRegistry registry = muleContext.getRegistry();

        // Someone is using a JAX-WS client directly and not going through MuleClient
        if (muleEndpoint == null)
        {
            return registry.lookupEndpointFactory().getOutboundEndpoint(uri);
        }
        
        // MuleClient/Dispatcher case
        EndpointURIEndpointBuilder builder = new EndpointURIEndpointBuilder(uri, muleContext);
        String connectorName = (String)muleEndpoint.getProperty("protocolConnector");
        if (connectorName != null) 
        {
            builder.setConnector(registry.lookupConnector(connectorName));
        }
        ep = registry.lookupEndpointFactory().getOutboundEndpoint(builder);
        protocolEndpoints.put(uri, ep);
        return ep;
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
            InputStream is = (InputStream) result.getPayload(InputStream.class);
            PushbackInputStream pb = new PushbackInputStream(is);
            result.setPayload(pb);
            
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
    
    protected String setupURL(Message message) throws MalformedURLException
    {
        String value = (String) message.get(Message.ENDPOINT_ADDRESS);
        String pathInfo = (String) message.get(Message.PATH_INFO);
        String queryString = (String) message.get(Message.QUERY_STRING);

        String result = value != null ? value : getTargetOrEndpoint();

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

    protected String getTargetOrEndpoint()
    {
        if (target != null)
        {
            return target.getAddress().getValue();
        }

        return endpoint.getAddress().toString();
    }

    public void onClose(final Message m) throws IOException
    {
    }
    
    protected MuleMessage sendStream(MessageAdapter sa, 
                                     OutboundEndpoint ep,
                                     Exchange exchange) throws MuleException
    {
        MuleEventContext eventContext = RequestContext.getEventContext();
        MuleSession session = null;
        if (eventContext != null)
        {
            session = eventContext.getSession();
        }

        MuleMessage message = new DefaultMuleMessage(sa);
        if (session == null)
        {
            session = new DefaultMuleSession(message, connector.getSessionHandler(), connector.getMuleContext());
        }
        
        // Filter out CXF client properties like wsdlLocation, inInterceptors, etc
        MuleEvent prev = RequestContext.getEvent();
        // If you're invoking from a CXF generated client, the event can be null
        if (prev != null) 
        {
            for (Iterator itr = prev.getEndpoint().getProperties().keySet().iterator(); itr.hasNext();)
            {
                String key = (String) itr.next();
                
                message.removeProperty(key);
            }
        }
        
        message.removeProperty(CxfConstants.OPERATION);
        message.removeProperty(CxfConstants.INBOUND_OPERATION);
        message.removeProperty(CxfConstants.INBOUND_SERVICE);
        
        MuleEvent event = new DefaultMuleEvent(message, ep, session, true);
        event.setTimeout(MuleEvent.TIMEOUT_NOT_SET_VALUE);
        RequestContext.setEvent(event);

        // This is a little "trick" to apply transformers from the CXF endpoint
        // to the raw message instead of the pojos
        if (applyTransformersToProtocol)
        {
            message.applyTransformers(((OutboundEndpoint) prev.getEndpoint()).getTransformers());
            
            // The underlying endpoint transformers
            event.transformMessage();
        }
        
        MuleMessage msg = ep.send(event);
        
        // We need to grab this back in the CxfMessageDispatcher again.
        Holder<MuleMessage> holder = (Holder<MuleMessage>) exchange.get("holder");
        // it's null if there is no dispatcher and the Client is being used directly over Mule
        if (holder != null)
        {
            holder.value = msg;
        }
        
        return msg;
    }

    public void close()
    {
        // in decoupled case, close response Destination if reference count
        // hits zero
        //
        if (decoupledDestination != null)
        {
            releaseDecoupledDestination();
        }
    }

    protected synchronized void duplicateDecoupledDestination()
    {
        decoupledDestinationRefCount++;
    }

    protected synchronized void releaseDecoupledDestination()
    {
        if (--decoupledDestinationRefCount == 0)
        {
            // LOG.log(Level.INFO, "shutting down decoupled destination");
            decoupledDestination.shutdown();
        }
    }

    public String getDecoupledEndpoint()
    {
        return decoupledEndpoint;
    }

    public void setDecoupledEndpoint(String decoupledEndpoint)
    {
        this.decoupledEndpoint = decoupledEndpoint;
    }

    /**
     * Get the target endpoint reference.
     * 
     * @param ei the corresponding EndpointInfo
     * @param t the given target EPR if available
     * @param bus the Bus
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
            inMessage.put(DECOUPLED_CHANNEL_MESSAGE, Boolean.TRUE);
            inMessage.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_OK);
            inMessage.remove(Message.ASYNC_POST_RESPONSE_DISPATCH);

            incomingObserver.onMessage(inMessage);
        }
    }
    public void setCloseInput(boolean closeInput)
    {
        this.closeInput = closeInput;
    }

    public void setApplyTransformersToProtocol(boolean applyTransformersToProtocol)
    {
        this.applyTransformersToProtocol = applyTransformersToProtocol;
    }

    protected CxfConnector getConnector()
    {
        return connector;
    }

    protected EndpointInfo getEndpoint()
    {
        return endpoint;
    }

    protected MuleUniversalTransport getTransport()
    {
        return transport;
    }
    
    public void setMuleEndpoint(ImmutableEndpoint muleEndpoint)
    {
        this.muleEndpoint = muleEndpoint;
        
    }
    
}
