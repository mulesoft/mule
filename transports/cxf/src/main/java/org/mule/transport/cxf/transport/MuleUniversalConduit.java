
package org.mule.transport.cxf.transport;

import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.RegistryContext;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.cxf.CxfConnector;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
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

    private static final Logger LOGGER = Logger.getLogger(MuleUniversalConduit.class.getName());

    private EndpointInfo endpoint;

    private CxfConnector connector;

    private Destination decoupledDestination;

    private String decoupledEndpoint;

    private MuleUniversalTransport transport;

    private int decoupledDestinationRefCount;

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

    private void setUpDecoupledDestination()
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
        final MuleUniversalConduit conduit = this;

        // Cache the message that CXF writes. We'll actually send the message
        // via the OutputHandler down below...
        CachedOutputStream out = new CachedOutputStream()
        {
            @Override
            public void close() throws IOException
            {
                super.close();

                // delegate to the onClose message for readability...
                conduit.onClose(message);
            }
        };

        message.setContent(OutputStream.class, out);
    }

    private String setupURL(Message message) throws MalformedURLException
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

    private String getTargetOrEndpoint()
    {
        if (target != null)
        {
            return target.getAddress().getValue();
        }

        return endpoint.getAddress().toString();
    }

    @SuppressWarnings("unchecked")
    public void onClose(final Message m) throws IOException
    {
        final CachedOutputStream cached = (CachedOutputStream) m.getContent(OutputStream.class);

        OutputHandler handler = new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                IOUtils.copy(cached.getInputStream(), out);
            }
        };

        // We can create a generic StreamMessageAdapter here as the underlying
        // transport will create one specific to the transport
        DefaultMessageAdapter req = new DefaultMessageAdapter(handler);
        String method = (String) m.get(Message.HTTP_REQUEST_METHOD);
        if (method == null) method = HttpConstants.METHOD_POST;

        req.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        req.setProperty(HttpConstants.HEADER_CONTENT_TYPE, m.get(Message.CONTENT_TYPE));
        
        // set all properties on the message adapter
        MuleEvent event = RequestContext.getEvent();
        if (event != null)
        {
            MuleMessage msg = event.getMessage();
            for (Iterator i = msg.getPropertyNames().iterator(); i.hasNext();)
            {
                String propertyName = (String) i.next();
                
             // But, don't copy mule message properties that should be on message but not on soap request
                // (MULE-2721)
                // Also see AxisMessageDispatcher.setCustomProperties()
                if (!(propertyName.startsWith(MuleProperties.PROPERTY_PREFIX)))
                {
                    req.setProperty(propertyName, msg.getProperty(propertyName));
                }
            }
        }
     
        MuleMessage result = null;

        String uri = setupURL(m);

        LOGGER.info("Sending message to " + uri);
        try
        {
            OutboundEndpoint ep = RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(uri);

            result = sendStream(req, ep);

            // If we have a result, send it back to CXF
            if (result != null)
            {
                Message inMessage = new MessageImpl();
                String contentType = req.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");

                inMessage.put(Message.ENCODING, result.getEncoding());
                inMessage.put(Message.CONTENT_TYPE, contentType);
                inMessage.setContent(InputStream.class, new ByteArrayInputStream(result.getPayloadAsBytes()));
                // inMessage.setContent(InputStream.class,
                // result.getPayload(InputStream.class));
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


    
    protected MuleMessage sendStream(MessageAdapter sa, OutboundEndpoint ep) throws MuleException
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

        MuleEvent event = new DefaultMuleEvent(message, ep, session, true);
        event.setTimeout(MuleEvent.TIMEOUT_NOT_SET_VALUE);
        RequestContext.setEvent(event);

        return ep.send(event);
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

    private synchronized void duplicateDecoupledDestination()
    {
        decoupledDestinationRefCount++;
    }

    private synchronized void releaseDecoupledDestination()
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
}
