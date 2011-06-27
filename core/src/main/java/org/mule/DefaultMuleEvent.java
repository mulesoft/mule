/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.ServiceType;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.management.stats.ProcessingTime;
import org.mule.security.MuleCredentials;
import org.mule.session.DefaultMuleSession;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.ObjectNameHelper;
import org.mule.util.UUID;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleEvent</code> represents any data event occurring in the Mule
 * environment. All data sent or received within the Mule environment will be passed
 * between components as an MuleEvent. <p/> The MuleEvent holds some data and provides
 * helper methods for obtaining the data in a format that the receiving Mule component
 * understands. The event can also maintain any number of properties that can be set
 * and retrieved by Mule components.
 */

public class DefaultMuleEvent extends EventObject implements MuleEvent, ThreadSafeAccess, DeserializationPostInitialisable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(DefaultMuleEvent.class);

    /**
     * The endpoint associated with the event
     */
    private transient InboundEndpoint endpoint = null;

    /**
     * the Universally Unique ID for the event
     */
    private String id = null;

    /**
     * The payload message used to read the payload of the event
     */
    private MuleMessage message = null;

    private MuleSession session;

    private boolean stopFurtherProcessing = false;

    private int timeout = TIMEOUT_NOT_SET_VALUE;

    private transient ResponseOutputStream outputStream = null;

    private transient Object transformedMessage = null;

    private Credentials credentials = null;

    protected String[] ignoredPropertyOverrides = new String[]{MuleProperties.MULE_METHOD_PROPERTY};

    private transient Map<String, Object> serializedData = null;

    private final ProcessingTime processingTime;
    
    private final MessageExchangePattern exchangePattern;

    /**
     * Properties cache that only reads properties once from the inbound message and
     * merges them with any properties on the endpoint. The message properties take
     * precedence over the endpoint properties
     *
     * @param message
     * @param endpoint
     * @param service
     * @param previousEvent
     */
    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            FlowConstruct service,
                            MuleEvent previousEvent)
    {
        super(message.getPayload());
        this.message = message;
        this.id = generateEventId();
        this.session = previousEvent.getSession();
        session.setFlowConstruct(service);
        this.endpoint = endpoint;
        this.timeout = previousEvent.getTimeout();
        this.outputStream = (ResponseOutputStream) previousEvent.getOutputStream();
        this.processingTime = ProcessingTime.newInstance(this.session, message.getMuleContext());
        this.exchangePattern = endpoint.getExchangePattern();
        fillProperties();
    }

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleEvent previousEvent,
                            MuleSession session)
    {
        super(message.getPayload());
        this.message = message;
        this.id = previousEvent.getId();
        this.session = session;
        this.endpoint = endpoint;
        this.timeout = previousEvent.getTimeout();
        this.outputStream = (ResponseOutputStream) previousEvent.getOutputStream();
        this.processingTime = ProcessingTime.newInstance(this.session, message.getMuleContext());
        this.exchangePattern = endpoint.getExchangePattern();
        fillProperties();
    }

    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            ResponseOutputStream outputStream,
                            OutboundEndpoint ep)
    {
        this(message, new NullInboundEndpoint(exchangePattern, message.getMuleContext()), session, outputStream);
        if (ep != null)
        {
            fillOutputProperties(ep);
        }
    }

    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, exchangePattern, session, outputStream, null);
    }

    public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, MuleSession session)
    {
        this(message, exchangePattern, session, null);
    }
    
    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleSession session)
    {
        this(message, endpoint, session, null, null);
    }

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleSession session,
                            ProcessingTime time)
    {
        this(message, endpoint, session, null, time);
    }

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, endpoint, session, outputStream, null);
    }

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleSession session,
                            ResponseOutputStream outputStream,
                            ProcessingTime time)
    {
        super(message.getPayload());
        this.message = message;
        this.endpoint = endpoint;
        this.session = session;
        this.id = generateEventId();
        this.outputStream = outputStream;
        this.exchangePattern = endpoint.getExchangePattern();
        fillProperties();
        this.processingTime = time != null ? time : ProcessingTime.newInstance(this.session, message.getMuleContext());
    }

    /**
     * A helper constructor used to rewrite an event payload
     *
     * @param message The message to use as the current payload of the event
     * @param rewriteEvent the previous event that will be used as a template for this event
     */
    public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent)
    {
        super(message.getPayload());
        this.message = message;
        this.id = rewriteEvent.getId();
        this.session = rewriteEvent.getSession();
        session.setFlowConstruct(rewriteEvent.getFlowConstruct());
        this.endpoint = rewriteEvent.getEndpoint();
        this.timeout = rewriteEvent.getTimeout();
        this.outputStream = (ResponseOutputStream) rewriteEvent.getOutputStream();
        this.exchangePattern = endpoint.getExchangePattern();
        if (rewriteEvent instanceof DefaultMuleEvent)
        {
            this.transformedMessage = ((DefaultMuleEvent) rewriteEvent).getCachedMessage();
            this.processingTime = ((DefaultMuleEvent)rewriteEvent).processingTime;
        }
        else
        {
            this.processingTime = ProcessingTime.newInstance(this.session, message.getMuleContext());
        }
        fillProperties();
    }

    protected void fillProperties()
    {
        fillProperties(endpoint, PropertyScope.INVOCATION);
    }

        /**
     * set the message's output properties from endpoint properties.
     */
    protected void fillOutputProperties(OutboundEndpoint ep)
    {
        fillProperties(ep, PropertyScope.OUTBOUND);
    }

    protected void fillProperties(ImmutableEndpoint ep, PropertyScope scope)
    {
        if (ep != null && ep.getProperties() != null)
        {
            for (Iterator<?> iterator = ep.getProperties().keySet().iterator(); iterator.hasNext();)
            {
                String prop = (String) iterator.next();
                Object value = ep.getProperties().get(prop);
                // don't overwrite property on the message
                if (!ignoreProperty(prop))
                {
                    //inbound endpoint properties are in the invocation scope
                    message.setProperty(prop, value, scope);
                }
            }
        }

        setCredentials(ep);
    }


    /**
     * This method is used to determine if a property on the previous event should be
     * ignored for the next event. This method is here because we don't have proper
     * scoped handling of meta data yet The rules are
     * <ol>
     * <li>If a property is already set on the current event don't overwrite with the previous event value
     * <li>If the property name appears in the ignoredPropertyOverrides list, then we always set it on the new event
     * </ol>
     *
     * @param key The name of the property to ignore
     * @return true if the property should be ignored, false otherwise
     */
    protected boolean ignoreProperty(String key)
    {
        if (key == null)
        {
            return true;
        }

        for (int i = 0; i < ignoredPropertyOverrides.length; i++)
        {
            if (key.equals(ignoredPropertyOverrides[i]))
            {
                return false;
            }
        }

        return null != message.getOutboundProperty(key);
    }

    protected void setCredentials(ImmutableEndpoint ep)
    {
        if (null != ep && null != ep.getEndpointURI() && null != ep.getEndpointURI().getUserInfo())
        {
            final String userName = ep.getEndpointURI().getUser();
            final String password = ep.getEndpointURI().getPassword();
            if (password != null && userName != null)
            {
                credentials = new MuleCredentials(userName, password.toCharArray());
            }
        }
    }

    @Override
    public Credentials getCredentials()
    {
        MuleCredentials creds = message.getOutboundProperty(MuleProperties.MULE_CREDENTIALS_PROPERTY);
        return (credentials != null ? credentials : creds);
    }

    Object getCachedMessage()
    {
        return transformedMessage;
    }

    @Override
    public MuleMessage getMessage()
    {
        return message;
    }

    @Override
    public byte[] getMessageAsBytes() throws DefaultMuleException
    {
        try
        {
            return message.getPayloadAsBytes();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(
                    CoreMessages.cannotReadPayloadAsBytes(message.getPayload().getClass().getName()), e);
        }
    }

    @Override
    @SuppressWarnings("cast")
    public <T> T transformMessage(Class<T> outputType) throws TransformerException
    {
        return (T) transformMessage(DataTypeFactory.create(outputType));
    }

    @Override
    public <T> T transformMessage(DataType<T> outputType) throws TransformerException
    {
        if (outputType == null)
        {
            throw new TransformerException(CoreMessages.objectIsNull("outputType"));
        }
        return message.getPayload(outputType);
    }

    /**
     * This method will attempt to convert the transformed message into an array of
     * bytes It will first check if the result of the transformation is a byte array
     * and return that. Otherwise if the the result is a string it will serialized
     * the CONTENTS of the string not the String object. finally it will check if the
     * result is a Serializable object and convert that to an array of bytes.
     *
     * @return a byte[] representation of the message
     * @throws TransformerException if an unsupported encoding is being used or if
     *                              the result message is not a String byte[] or Seializable object
     * @deprecated use {@link #transformMessage(org.mule.api.transformer.DataType)} instead
     */
    @Override
    @Deprecated
    public byte[] transformMessageToBytes() throws TransformerException
    {
        return transformMessage(DataType.BYTE_ARRAY_DATA_TYPE);
    }

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into a String. The transformer used is the one configured on the endpoint
     * through which this event was received.
     *
     * @return the message transformed into it's recognised or expected format as a
     *         Strings.
     * @throws org.mule.api.transformer.TransformerException
     *          if a failure occurs in
     *          the transformer
     * @see org.mule.api.transformer.Transformer
     */
    @Override
    public String transformMessageToString() throws TransformerException
    {
        return transformMessage(DataTypeFactory.createWithEncoding(String.class, getEncoding()));
    }

    @Override
    public String getMessageAsString() throws MuleException
    {
        return getMessageAsString(getEncoding());
    }

    /**
     * Returns the message contents for logging
     *
     * @param encoding the encoding to use when converting bytes to a string, if necessary
     * @return the message contents as a string
     * @throws org.mule.api.MuleException if the message cannot be converted into a
     *                                    string
     */
    @Override
    public String getMessageAsString(String encoding) throws MuleException
    {
        try
        {
            return message.getPayloadForLogging(encoding);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(
                    CoreMessages.cannotReadPayloadAsString(message.getClass().getName()), e);
        }
    }

    @Override
    public String getId()
    {
        return id;
    }

    /**
     * @see #getMessage()
     * @deprecated use appropriate scope-aware calls on the MuleMessage (via event.getMessage())
     */
    @Override
    @Deprecated
    public Object getProperty(String name)
    {
        throw new UnsupportedOperationException("Method's behavior has changed in Mule 3, use " +
                                                "event.getMessage() and suitable scope-aware property access " +
                                                "methods on it");
    }

    /**
     * @see #getMessage()
     * @deprecated use appropriate scope-aware calls on the MuleMessage (via event.getMessage())
     */
    @Override
    @Deprecated
    public Object getProperty(String name, Object defaultValue)
    {
        throw new UnsupportedOperationException("Method's behavior has changed in Mule 3, use " +
                                                "event.getMessage() and suitable scope-aware property access " +
                                                "methods on it");
    }

    @Override
    public InboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer(64);
        buf.append("MuleEvent: ").append(getId());
        buf.append(", stop processing=").append(isStopFurtherProcessing());
        buf.append(", ").append(endpoint);

        return buf.toString();
    }

    protected String generateEventId()
    {
        return UUID.getUUID();
    }

    @Override
    public MuleSession getSession()
    {
        return session;
    }

    void setSession(MuleSession session)
    {
        this.session = session;
    }

    /**
     * Gets the recipient service of this event
     */
    @Override
    public FlowConstruct getFlowConstruct()
    {
        return session.getFlowConstruct();
    }

    /**
     * Determines whether the default processing for this event will be executed
     *
     * @return Returns the stopFurtherProcessing.
     */
    @Override
    public boolean isStopFurtherProcessing()
    {
        return stopFurtherProcessing;
    }

    /**
     * Setting this parameter will stop the Mule framework from processing this event
     * in the standard way. This allow for client code to override default behaviour.
     * The common reasons for doing this are - 1. The service has more than one send
     * endpoint configured; the service must dispatch to other prviders
     * programmatically by using the service on the current event 2. The service doesn't
     * send the current event out through a endpoint. i.e. the processing of the
     * event stops in the uMO.
     *
     * @param stopFurtherProcessing The stopFurtherProcessing to set.
     */
    @Override
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        this.stopFurtherProcessing = stopFurtherProcessing;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof DefaultMuleEvent))
        {
            return false;
        }

        final DefaultMuleEvent event = (DefaultMuleEvent) o;

        if (message != null ? !message.equals(event.message) : event.message != null)
        {
            return false;
        }
        return id.equals(event.id);
    }

    @Override
    public int hashCode()
    {
        return 29 * id.hashCode() + (message != null ? message.hashCode() : 0);
    }

    @Override
    public int getTimeout()
    {
        if (timeout == TIMEOUT_NOT_SET_VALUE)
        {
            // If this is not set it will use the default timeout value
            timeout = endpoint.getResponseTimeout();
        }
        return timeout;
    }

    @Override
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    /**
     * An output stream can optionally be used to write response data to an incoming
     * message.
     *
     * @return an output strem if one has been made available by the message receiver
     *         that received the message
     */
    @Override
    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        if (serializedData != null)
        {
            out.writeInt((Integer)serializedData.get("endpointHashcode"));
            out.writeObject(serializedData.get("endpointBuilderName"));
            out.writeObject(serializedData.get("endpointUri"));
            List transformers = (List) serializedData.get("transformers");
            out.writeInt(transformers.size());
            for (Object transformer : transformers)
            {
                out.writeObject(transformer);
            }
        }
        else
        {
            out.writeInt(endpoint.hashCode());
            out.writeObject(endpoint.getEndpointBuilderName());

            String uri = endpoint.getEndpointURI().getUri().toString();

            if (endpoint instanceof NullInboundEndpoint
                || ObjectNameHelper.isDefaultAutoGeneratedConnector(endpoint.getConnector()))
            {
                // If connector was auto-generated then don't serialize endpoint with
                // connector name. Once deserialized it should
                // auto-discover/auto-generate connector again using same process.
                out.writeObject(uri);
            }
            else
            {
                // make sure to write out the connector's name along with the endpoint URI. Omitting the
                // connector will fail rebuilding the endpoint when this event is read back in and there
                // is more than one connector for the protocol.
                out.writeObject(uri + "?connector=" + endpoint.getConnector().getName());
            }

            // write number of Transformers
            out.writeInt(endpoint.getTransformers().size());

            // write transformer names if necessary
            if (endpoint.getTransformers().size() > 0)
            {
                for (Transformer transformer : endpoint.getTransformers())
                {
                    out.writeObject(transformer.getName());
                }
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException, MuleException
    {
        in.defaultReadObject();
        serializedData = new HashMap<String, Object>();
        serializedData.put("endpointHashcode", in.readInt());
        serializedData.put("endpointBuilderName", in.readObject());
        serializedData.put("endpointUri", in.readObject());
        int count = in.readInt();

        List<String> transformerNames = new LinkedList<String>();
        if (count > 0)
        {
            while (--count > 0)
            {
                transformerNames.add((String) in.readObject());
            }
        }
        serializedData.put("transformers", transformerNames);
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked
     * after the object has been deserialized passing in the current MuleContext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    @SuppressWarnings({"unused", "unchecked"})
    private void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        // this method can be called even on objects that were not serialized. In this case,
        // the temporary holder for serialized data is not initialized and we can just return
        if (serializedData == null)
        {
            return;
        }

        if (session instanceof DefaultMuleSession)
        {
            ((DefaultMuleSession) session).initAfterDeserialisation(muleContext);
        }
        if (message instanceof DefaultMuleMessage)
        {
            ((DefaultMuleMessage) message).initAfterDeserialisation(muleContext);
        }
        int endpointHashcode = (Integer) serializedData.get("endpointHashcode");
        String endpointBuilderName = (String) serializedData.get("endpointBuilderName");
        String endpointUri = (String) serializedData.get("endpointUri");
        List<String> transformerNames = (List<String>) serializedData.get("transformers");

        // 1) First attempt to get same endpoint instance from registry using
        // hashcode, this will work if registry hasn't been disposed.
        endpoint = (InboundEndpoint) muleContext.getRegistry().lookupObject(
                DefaultEndpointFactory.ENDPOINT_REGISTRY_PREFIX + endpointHashcode);

        // Registry has been disposed so we need to recreate endpoint
        if (endpoint == null)
        {
            // 2) If endpoint references it's builder and this is available then use
            // the builder to recreate the endpoint
            if ((endpointBuilderName != null)
                    && muleContext.getRegistry().lookupEndpointBuilder(endpointBuilderName) != null)
            {
                    endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
                            endpointBuilderName);
            }
            // 3) Otherwise recreate using endpoint uri string and transformers. (As in 1.4)
            else
            {
                List<Transformer> transformers = new LinkedList<Transformer>();
                for (String name : transformerNames)
                {
                    Transformer next = muleContext.getRegistry().lookupTransformer(name);
                    if (next == null)
                    {
                        throw new IllegalStateException(CoreMessages.objectNotFound(name).toString());
                    }
                    else
                    {
                        transformers.add(next);
                    }
                }
                EndpointURI uri = new MuleEndpointURI(endpointUri, muleContext);

                TransportServiceDescriptor tsd = (TransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, uri.getFullScheme(), null);
                EndpointBuilder endpointBuilder = tsd.createEndpointBuilder(endpointUri);
                endpointBuilder.setTransformers(transformers);

                    endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
                            endpointBuilder);
            }
        }

        serializedData = null;
    }

    /**
     * Gets the encoding for this message. First it looks to see if encoding has been
     * set on the endpoint, if not it will check the message itself and finally it
     * will fall back to the Mule global configuration for encoding which cannot be
     * null.
     *
     * @return the encoding for the event
     */
    @Override
    public String getEncoding()
    {
        String encoding = message.getEncoding();
        if (encoding == null)
        {
            encoding = endpoint.getEncoding();
        }

        return encoding;
    }

    @Override
    public MuleContext getMuleContext()
    {
        return message.getMuleContext();
    }

    @Override
    public ThreadSafeAccess newThreadCopy()
    {
        if (message instanceof ThreadSafeAccess)
        {
            DefaultMuleEvent copy = new DefaultMuleEvent((MuleMessage) ((ThreadSafeAccess) message).newThreadCopy(), this);
            copy.resetAccessControl();
            return copy;
        }
        else
        {
            return this;
        }
    }

    @Override
    public void resetAccessControl()
    {
        if (message instanceof ThreadSafeAccess)
        {
            ((ThreadSafeAccess) message).resetAccessControl();
        }
    }

    @Override
    public void assertAccess(boolean write)
    {
        if (message instanceof ThreadSafeAccess)
        {
            ((ThreadSafeAccess) message).assertAccess(write);
        }
    }

    @Override
    @Deprecated
    public Object transformMessage() throws TransformerException
    {
        logger.warn("Deprecation warning: MuleEvent.transformMessage does nothing in Mule 3.x.  The message is already transformed before the event reaches a component");
        return message.getPayload();
    }

    @Override
    public ProcessingTime getProcessingTime()
    {
        return processingTime;
    }

    private static class NullInboundEndpoint extends DefaultInboundEndpoint
    {
        public NullInboundEndpoint(MessageExchangePattern mep, MuleContext muleContext)
        {
            super(null, new MuleEndpointURI("dynamic://null", null, null, null, null, null,
                URI.create("dynamic://null"), muleContext), null, new HashMap(), new MuleTransactionConfig(),
                false, mep, 0, null, null, null, muleContext, null, null, null, null, false, null);
        }

    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return exchangePattern;
    }
}
