/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.ProcessingDescriptor;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.ProcessingTime;
import org.mule.security.MuleCredentials;
import org.mule.session.DefaultMuleSession;
import org.mule.transaction.TransactionCoordination;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleEvent</code> represents any data event occurring in the Mule environment. All data sent or
 * received within the Mule environment will be passed between components as an MuleEvent.
 * <p/>
 * The MuleEvent holds some data and provides helper methods for obtaining the data in a format that the
 * receiving Mule component understands. The event can also maintain any number of flowVariables that can be
 * set and retrieved by Mule components.
 */

public class DefaultMuleEvent implements MuleEvent, ThreadSafeAccess, DeserializationPostInitialisable
{
    private static final long serialVersionUID = 1L;

    private static Log logger = LogFactory.getLog(DefaultMuleEvent.class);

    /** Immutable MuleEvent state **/

    /** The Universally Unique ID for the event */
    private final String id;
    private MuleMessage message;
    private final MuleSession session;
    private transient FlowConstruct flowConstruct;

    private final Credentials credentials;
    private final String encoding;
    private final MessageExchangePattern exchangePattern;
    private final URI messageSourceURI;
    private final String messageSourceName;
    private final ReplyToHandler replyToHandler;
    private final boolean transacted;
    private final boolean synchronous;

    /** Mutable MuleEvent state **/
    private boolean stopFurtherProcessing = false;
    private int timeout = TIMEOUT_NOT_SET_VALUE;
    private transient ResponseOutputStream outputStream;
    private final ProcessingTime processingTime;
    private Object replyToDestination;

    protected String[] ignoredPropertyOverrides = new String[]{MuleProperties.MULE_METHOD_PROPERTY};
    private boolean notificationsEnabled = true;

    private transient Map<String, Object> serializedData = null;

    private CopyOnWriteCaseInsensitiveMap<String, Object> flowVariables = new CopyOnWriteCaseInsensitiveMap<String, Object>();

    // Constructors

    /**
     * Constructor used to create a message with no message source with minimal arguments
     */
    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session)
    {
        this(message, exchangePattern, flowConstruct, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, null);
    }

    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct)
    {
        this(message, exchangePattern, flowConstruct, new DefaultMuleSession(), message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, null);
    }

    /**
     * Constructor used to create a message with no message source with minimal arguments and
     * ResponseOutputStream
     */
    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, exchangePattern, flowConstruct, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, outputStream);
    }

    /**
     * Constructor used to create a message with no message source with all additional arguments
     */
    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream)
    {
        this(message, URI.create("none"), exchangePattern, flowConstruct, session, timeout, credentials,
            outputStream);
    }

    /**
     * Constructor used to create a message with a uri that idendifies the message source with minimal
     * arguments
     */
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session)
    {
        this(message, messageSourceURI, exchangePattern, flowConstruct, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, null);
    }

    /**
     * Constructor used to create a message with a uri that idendifies the message source with minimal
     * arguments and ResponseOutputStream
     */
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, messageSourceURI, exchangePattern, flowConstruct, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, outputStream);
    }

    /**
     * Constructor used to create a message with a identifiable message source with all additional arguments
     */
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream)
    {
        this.id = generateEventId(message.getMuleContext());
        this.flowConstruct = flowConstruct;
        this.session = session;
        setMessage(message);

        this.exchangePattern = exchangePattern;
        this.outputStream = outputStream;
        this.credentials = null;
        this.encoding = message.getMuleContext().getConfiguration().getDefaultEncoding();
        this.messageSourceName = messageSourceURI.toString();
        this.messageSourceURI = messageSourceURI;
        this.processingTime = ProcessingTime.newInstance(this);
        this.replyToHandler = null;
        this.replyToDestination = null;
        this.timeout = timeout;
        this.transacted = false;
        this.synchronous = resolveEventSynchronicity();
    }

    // Constructors for inbound endpoint

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            FlowConstruct flowConstruct,
                            MuleSession session)
    {
        this(message, endpoint, flowConstruct, session, null, null, null);
    }

    public DefaultMuleEvent(MuleMessage message, InboundEndpoint endpoint, FlowConstruct flowConstruct)
    {
        this(message, endpoint, flowConstruct, new DefaultMuleSession(), null, null, null);
    }

    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            ReplyToHandler replyToHandler,
                            Object replyToDestination,
                            ResponseOutputStream outputStream)
    {
        this.id = generateEventId(message.getMuleContext());
        this.flowConstruct = flowConstruct;
        this.session = session;
        setMessage(message);

        this.outputStream = outputStream;
        this.processingTime = ProcessingTime.newInstance(this);
        this.replyToHandler = replyToHandler;
        this.replyToDestination = replyToDestination;
        this.credentials = extractCredentials(endpoint);
        this.encoding = endpoint.getEncoding();
        this.exchangePattern = endpoint.getExchangePattern();
        this.messageSourceName = endpoint.getName();
        this.messageSourceURI = endpoint.getEndpointURI().getUri();
        this.timeout = endpoint.getResponseTimeout();
        this.transacted = endpoint.getTransactionConfig().isTransacted();
        fillProperties(endpoint);
        this.synchronous = resolveEventSynchronicity();
    }

    // Constructors to copy MuleEvent

    /**
     * A helper constructor used to rewrite an event payload
     *
     * @param message The message to use as the current payload of the event
     * @param rewriteEvent the previous event that will be used as a template for this event
     */
    public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent)
    {
        this(message, rewriteEvent, rewriteEvent.getSession());
    }

    public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct)
    {
        this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(),
            rewriteEvent.isSynchronous());
    }

    public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct, ReplyToHandler replyToHandler, Object replyToDestination)
    {
        this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(),
             rewriteEvent.isSynchronous(), replyToHandler, replyToDestination, true);
    }

    public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus)
    {
        this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus);
    }

    public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareVars)
    {
        this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus, shareVars);
    }

    /**
     * A helper constructor used to rewrite an event payload
     *
     * @param message The message to use as the current payload of the event
     * @param rewriteEvent the previous event that will be used as a template for this event
     */
    public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, MuleSession session)
    {
        this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), session, rewriteEvent.isSynchronous());
    }

    protected DefaultMuleEvent(MuleMessage message,
                               MuleEvent rewriteEvent,
                               FlowConstruct flowConstruct,
                               MuleSession session,
                               boolean synchronous)
    {
        this(message, rewriteEvent, flowConstruct, session, synchronous, rewriteEvent.getReplyToHandler(), rewriteEvent.getReplyToDestination(), true);
    }

    protected DefaultMuleEvent(MuleMessage message,
                               MuleEvent rewriteEvent,
                               FlowConstruct flowConstruct,
                               MuleSession session,
                               boolean synchronous,
                               boolean shareVars)
    {
        this(message, rewriteEvent, flowConstruct, session, synchronous, rewriteEvent.getReplyToHandler(), rewriteEvent.getReplyToDestination(), shareVars);
    }

    protected DefaultMuleEvent(MuleMessage message,
                               MuleEvent rewriteEvent,
                               FlowConstruct flowConstruct,
                               MuleSession session,
                               boolean synchronous,
                               ReplyToHandler replyToHandler,
                               Object replyToDestination,
                               boolean shareVars)
    {
        this.id = rewriteEvent.getId();
        this.flowConstruct = flowConstruct;

        this.credentials = rewriteEvent.getCredentials();
        this.encoding = rewriteEvent.getEncoding();
        this.exchangePattern = rewriteEvent.getExchangePattern();
        this.messageSourceName = rewriteEvent.getMessageSourceName();
        this.messageSourceURI = rewriteEvent.getMessageSourceURI();
        this.outputStream = (ResponseOutputStream) rewriteEvent.getOutputStream();
        if (rewriteEvent instanceof DefaultMuleEvent)
        {
            this.processingTime = ((DefaultMuleEvent) rewriteEvent).processingTime;
            if (shareVars)
            {
                this.flowVariables = ((DefaultMuleEvent) rewriteEvent).flowVariables;
                this.session = session;
            }
            else
            {
                this.flowVariables.putAll(((DefaultMuleEvent) rewriteEvent).flowVariables);
                this.session = new DefaultMuleSession(session);
            }
        }
        else
        {
            this.processingTime = ProcessingTime.newInstance(this);
            this.session = session;
        }
        setMessage(message);
        this.replyToHandler = replyToHandler;
        this.replyToDestination = replyToDestination;
        this.timeout = rewriteEvent.getTimeout();
        this.transacted = rewriteEvent.isTransacted();
        this.notificationsEnabled = rewriteEvent.isNotificationsEnabled();
        this.synchronous = synchronous;
    }

    // Constructor with everything just in case

    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            String messageSourceName,
                            MessageExchangePattern exchangePattern,
                            FlowConstruct flowConstruct,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream,
                            String encoding,
                            boolean transacted,
                            boolean synchronous,
                            Object replyToDestination,
                            ReplyToHandler replyToHandler)
    {
        this.id = generateEventId(message.getMuleContext());
        this.flowConstruct = flowConstruct;
        this.session = session;
        setMessage(message);

        this.credentials = credentials;
        this.encoding = encoding;
        this.exchangePattern = exchangePattern;
        this.messageSourceURI = messageSourceURI;
        this.messageSourceName = messageSourceName;
        this.processingTime = ProcessingTime.newInstance(this);
        this.replyToHandler = replyToHandler;
        this.replyToDestination = replyToDestination;
        this.transacted = transacted;
        this.synchronous = synchronous;
        this.timeout = timeout;
        this.outputStream = outputStream;
    }

    protected boolean resolveEventSynchronicity()
    {
        boolean syncProcessingStrategy = false;
        if (flowConstruct != null && flowConstruct instanceof ProcessingDescriptor)
        {
            syncProcessingStrategy = ((ProcessingDescriptor) flowConstruct).isSynchronous();
        }
        
        return transacted
               || exchangePattern.hasResponse()
               || message.getProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY,
                   PropertyScope.INBOUND, Boolean.FALSE) || syncProcessingStrategy;
    }

    protected void fillProperties(InboundEndpoint endpoint)
    {
        if (endpoint != null && endpoint.getProperties() != null)
        {
            for (Iterator<?> iterator = endpoint.getProperties().keySet().iterator(); iterator.hasNext();)
            {
                String prop = (String) iterator.next();

                // don't overwrite property on the message
                if (!ignoreProperty(prop))
                {
                    // inbound endpoint flowVariables are in the invocation scope
                    Object value = endpoint.getProperties().get(prop);
                    message.setInvocationProperty(prop, value);
                }
            }
        }
    }

    /**
     * This method is used to determine if a property on the previous event should be ignored for the next
     * event. This method is here because we don't have proper scoped handling of meta data yet The rules are
     * <ol>
     * <li>If a property is already set on the current event don't overwrite with the previous event value
     * <li>If the property name appears in the ignoredPropertyOverrides list, then we always set it on the new
     * event
     * </ol>
     *
     * @param key The name of the property to ignore
     * @return true if the property should be ignored, false otherwise
     */
    protected boolean ignoreProperty(String key)
    {
        if (key == null || key.startsWith(MuleProperties.ENDPOINT_PROPERTY_PREFIX))
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

    protected Credentials extractCredentials(InboundEndpoint endpoint)
    {
        if (null != endpoint && null != endpoint.getEndpointURI()
            && null != endpoint.getEndpointURI().getUserInfo())
        {
            final String userName = endpoint.getEndpointURI().getUser();
            final String password = endpoint.getEndpointURI().getPassword();
            if (password != null && userName != null)
            {
                return new MuleCredentials(userName, password.toCharArray());
            }
        }
        return null;
    }

    @Override
    public Credentials getCredentials()
    {
        MuleCredentials creds = message.getOutboundProperty(MuleProperties.MULE_CREDENTIALS_PROPERTY);
        return (credentials != null ? credentials : creds);
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
            throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsBytes(message.getPayload()
                .getClass()
                .getName()), e);
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
     * This method will attempt to convert the transformed message into an array of bytes It will first check
     * if the result of the transformation is a byte array and return that. Otherwise if the the result is a
     * string it will serialized the CONTENTS of the string not the String object. finally it will check if
     * the result is a Serializable object and convert that to an array of bytes.
     *
     * @return a byte[] representation of the message
     * @throws TransformerException if an unsupported encoding is being used or if the result message is not a
     *             String byte[] or Seializable object
     * @deprecated use {@link #transformMessage(org.mule.api.transformer.DataType)} instead
     */
    @Override
    @Deprecated
    public byte[] transformMessageToBytes() throws TransformerException
    {
        return transformMessage(DataType.BYTE_ARRAY_DATA_TYPE);
    }

    /**
     * Returns the message transformed into it's recognised or expected format and then into a String. The
     * transformer used is the one configured on the endpoint through which this event was received.
     *
     * @return the message transformed into it's recognised or expected format as a Strings.
     * @throws org.mule.api.transformer.TransformerException if a failure occurs in the transformer
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
     * @throws org.mule.api.MuleException if the message cannot be converted into a string
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
            throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsString(message.getClass()
                .getName()), e);
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
        throw new UnsupportedOperationException(
            "Method's behavior has changed in Mule 3, use "
                            + "event.getMessage() and suitable scope-aware property access "
                            + "methods on it");
    }

    /**
     * @see #getMessage()
     * @deprecated use appropriate scope-aware calls on the MuleMessage (via event.getMessage())
     */
    @Override
    @Deprecated
    public Object getProperty(String name, Object defaultValue)
    {
        throw new UnsupportedOperationException(
            "Method's behavior has changed in Mule 3, use "
                            + "event.getMessage() and suitable scope-aware property access "
                            + "methods on it");
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(64);
        buf.append("MuleEvent: ").append(getId());
        buf.append(", stop processing=").append(isStopFurtherProcessing());
        buf.append(", ").append(messageSourceURI);

        return buf.toString();
    }

    protected String generateEventId(MuleContext context)
    {
        return context.getUniqueIdString();
    }

    @Override
    public MuleSession getSession()
    {
        return session;
    }

    /**
     * Gets the recipient service of this event
     */
    @Override
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
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
     * Setting this parameter will stop the Mule framework from processing this event in the standard way.
     * This allow for client code to override default behaviour. The common reasons for doing this are - 1.
     * The service has more than one send endpoint configured; the service must dispatch to other prviders
     * programmatically by using the service on the current event 2. The service doesn't send the current
     * event out through a endpoint. i.e. the processing of the event stops in the uMO.
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
        if (getMuleContext().getConfiguration().isDisableTimeouts())
        {
            return TIMEOUT_WAIT_FOREVER;
        }
        if (timeout == TIMEOUT_NOT_SET_VALUE)
        {
            return message.getMuleContext().getConfiguration().getDefaultResponseTimeout();
        }
        else
        {
            return timeout;
        }
    }

    @Override
    public void setTimeout(int timeout)
    {
        if (timeout >= 0)
        {
            this.timeout = timeout;
        }
    }

    /**
     * An output stream can optionally be used to write response data to an incoming message.
     *
     * @return an output strem if one has been made available by the message receiver that received the
     *         message
     */
    @Override
    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked after the
     * object has been deserialized passing in the current MuleContext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    @SuppressWarnings({"unused"})
    private void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        if (message instanceof DefaultMuleMessage)
        {
            ((DefaultMuleMessage) message).initAfterDeserialisation(muleContext);
            setMessage(message);
        }
        if (replyToHandler instanceof DefaultReplyToHandler)
        {
            ((DefaultReplyToHandler) replyToHandler).initAfterDeserialisation(muleContext);
        }
        if (replyToDestination instanceof DeserializationPostInitialisable)
        {
            try
            {
                DeserializationPostInitialisable.Implementation.init(replyToDestination, muleContext);
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }

        }
        // this method can be called even on objects that were not serialized. In this case,
        // the temporary holder for serialized data is not initialized and we can just return
        if (serializedData == null)
        {
            return;
        }

        String serviceName = this.getTransientServiceName();
        // Can be null if service call originates from MuleClient
        if (serviceName != null)
        {
            flowConstruct = muleContext.getRegistry().lookupFlowConstruct(serviceName);
        }
        serializedData = null;
    }

    /**
     * Gets the encoding for this message. First it looks to see if encoding has been set on the endpoint, if
     * not it will check the message itself and finally it will fall back to the Mule global configuration for
     * encoding which cannot be null.
     *
     * @return the encoding for the event
     */
    @Override
    public String getEncoding()
    {
        if (message.getEncoding() != null)
        {
            return message.getEncoding();
        }
        else
        {
            return encoding;
        }
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
            DefaultMuleEvent copy = new DefaultMuleEvent(
                (MuleMessage) ((ThreadSafeAccess) message).newThreadCopy(), this);
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

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return exchangePattern;
    }

    @Override
    public boolean isTransacted()
    {
        return transacted || TransactionCoordination.getInstance().getTransaction() != null;
    }

    @Override
    public URI getMessageSourceURI()
    {
        return messageSourceURI;
    }

    @Override
    public String getMessageSourceName()
    {
        return messageSourceName;
    }

    @Override
    public ReplyToHandler getReplyToHandler()
    {
        return replyToHandler;
    }

    @Override
    public Object getReplyToDestination()
    {
        return replyToDestination;
    }

    @Override
    public void captureReplyToDestination()
    {

    }

    @Override
    public boolean isSynchronous()
    {
        return synchronous;
    }

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        // Can be null if service call originates from MuleClient
        if (serializedData != null)
        {
            Object serviceName = serializedData.get("serviceName");
            if (serviceName != null)
            {
                out.writeObject(serviceName);
            }
        }
        else
        {
            if (getFlowConstruct() != null)
            {
                out.writeObject(getFlowConstruct() != null ? getFlowConstruct().getName() : "null");
            }
        }
        for (Map.Entry<String, Object> entry : flowVariables.entrySet())
        {
            Object value = entry.getValue();
            if (value != null && !(value instanceof Serializable))
            {
                String message = String.format(
                    "Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
                logger.error(message);
                throw new IOException(message);
            }
        }

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        serializedData = new HashMap<String, Object>();

        try
        {
            // Optional
            this.setTransientServiceName(in.readObject());
        }
        catch (OptionalDataException e)
        {
            // ignore
        }
    }
    
    /**
     * Used to fetch the {@link #flowConstruct} after deserealization since its a
     * transient value. This is not part of the public API and should only be used
     * internally for serialization/deserialization
     * 
     * @param serviceName the name of the service
     */
    public void setTransientServiceName(Object serviceName)
    {
        if (serializedData == null)
        {
            serializedData = new HashMap<String, Object>();
        }
        serializedData.put("serviceName", serviceName);
    }

    private String getTransientServiceName()
    {
        return serializedData != null ? (String) serializedData.get("serviceName") : null;
    }

    @Override
    public void setMessage(MuleMessage message)
    {
        this.message = message;
        if (message instanceof DefaultMuleMessage)
        {
            // Don't copy properties from message to event every time we copy event as we did before. Rather
            // only copy invocation properties over if MuleMessage had invocation properties set on it before
            // MuleEvent was created.
            flowVariables.putAll(((DefaultMuleMessage) message).getOrphanFlowVariables());
            
            ((DefaultMuleMessage) message).setInvocationProperties(flowVariables);
            if (session instanceof DefaultMuleSession)
            {
                ((DefaultMuleMessage) message).setSessionProperties(((DefaultMuleSession) session).getProperties());
            }
        }
    }

    /**
     * This method does a complete deep copy of the event.
     *
     * This method should be used whenever the event is going to be executed
     * in a different context and changes to that event must not effect the source event.
     *
     * @param event the event that must be copied
     * @return the copied event
     */
    public static MuleEvent copy(MuleEvent event)
    {
        MuleMessage messageCopy = (MuleMessage) ((ThreadSafeAccess) event.getMessage()).newThreadCopy();
        DefaultMuleEvent eventCopy = new DefaultMuleEvent(messageCopy, event, new DefaultMuleSession(
            event.getSession()));
        eventCopy.flowVariables = ((DefaultMuleEvent) event).flowVariables.clone();
        ((DefaultMuleMessage) messageCopy).setInvocationProperties(eventCopy.flowVariables);
        ((DefaultMuleMessage) messageCopy).resetAccessControl();
        return eventCopy;
    }

    @Override
    public Set<String> getFlowVariableNames()
    {
        return flowVariables.keySet();
    }

    @Override
    public void clearFlowVariables()
    {
        flowVariables.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getFlowVariable(String key)
    {
        return (T) flowVariables.get(key);
    }

    @Override
    public void setFlowVariable(String key, Object value)
    {
        flowVariables.put(key, value);
    }

    @Override
    public void removeFlowVariable(String key)
    {
        flowVariables.remove(key);
    }

    @Override
    public <T> T getSessionVariable(String key)
    {
        return session.<T> getProperty(key);
    }

    @Override
    public void setSessionVariable(String key, Object value)
    {
        session.setProperty(key, value);
    }

    @Override
    public void removeSessionVariable(String key)
    {
        session.removeProperty(key);
    }

    @Override
    public Set<String> getSessionVariableNames()
    {
        return session.getPropertyNamesAsSet();
    }

    @Override
    public void clearSessionVariables()
    {
        session.clearProperties();
    }

    // Deprecated constructors

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, MuleSession session)
    {
        this(message, exchangePattern, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, null);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, exchangePattern, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, outputStream);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream)
    {
        this(message, URI.create("none"), exchangePattern, session, timeout, credentials, outputStream);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            MuleSession session)
    {
        this(message, messageSourceURI, exchangePattern, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, null);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            ResponseOutputStream outputStream)
    {
        this(message, messageSourceURI, exchangePattern, session, message.getMuleContext()
            .getConfiguration()
            .getDefaultResponseTimeout(), null, outputStream);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream)
    {
        this(message, messageSourceURI, exchangePattern, session.getFlowConstruct(), session, timeout,
            credentials, outputStream);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message, InboundEndpoint endpoint, MuleSession session)
    {
        this(message, endpoint, session, null, null, null);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            InboundEndpoint endpoint,
                            MuleSession session,
                            ReplyToHandler replyToHandler,
                            ResponseOutputStream outputStream,
                            Object replyToDestination)
    {
        this(message, endpoint, session.getFlowConstruct(), session, replyToHandler, replyToDestination, outputStream);
    }

    /**
     * WARNING: Only use this constructor if the instance of MuleSession has a flowConstruct set. This
     * constructor is only here for backwards compatibility.
     */
    @Deprecated
    public DefaultMuleEvent(MuleMessage message,
                            URI messageSourceURI,
                            String messageSourceName,
                            MessageExchangePattern exchangePattern,
                            MuleSession session,
                            int timeout,
                            Credentials credentials,
                            ResponseOutputStream outputStream,
                            String encoding,
                            boolean transacted,
                            boolean synchronous,
                            Object replyToDestination,
                            ReplyToHandler replyToHandler)
    {
        this(message, messageSourceURI, messageSourceName, exchangePattern, session.getFlowConstruct(),
            session, timeout, credentials, outputStream, encoding, transacted, synchronous,
            replyToDestination, replyToHandler);
    }

    @Override
    public boolean isNotificationsEnabled()
    {
        return notificationsEnabled;
    }

    @Override
    public void setEnableNotifications(boolean enabled)
    {
        notificationsEnabled = enabled;
    }
}
