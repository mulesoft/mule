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
 *
 */
package org.mule.impl;

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.security.MuleCredentials;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.PropertiesHelper;
import org.mule.util.UUID;
import org.mule.util.Utility;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>MuleEvent</code> represents any data event occuring in the Mule
 * environment. All data sent or received within the mule environment will be
 * passed between components as an UMOEvent. <p/> The UMOEvent holds some data
 * and provides helper methods for obtaining the data in a format that the
 * receiving Mule UMO understands. The event can also maintain any number of
 * properties that can be set and retrieved by Mule UMO components.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleEvent extends EventObject implements UMOEvent
{
    /**
     * The endpoint associated with the event
     */
    private transient UMOEndpoint endpoint = null;

    /**
     * the Universally Unique ID for the event
     */
    private String id = null;

    /**
     * The payload message used to read the payload payload of the event
     */
    private UMOMessage message = null;

    private transient UMOSession session;

    private boolean stopFurtherProcessing = false;

    private boolean synchronous = false;

    private int timeout = TIMEOUT_NOT_SET_VALUE;

    private transient ResponseOutputStream outputStream = null;

    private transient Object transformedMessage = null;

    private UMOCredentials credentials = null;

    /**
     * Properties cache that only reads properties once from the inbound message
     * and merges them with any properties on the endpoint. The message
     * properties take precedence over the endpoint properties
     */
    private Map properties = new HashMap();

    public MuleEvent(UMOMessage message, UMOEndpoint endpoint, UMOComponent component, UMOEvent previousEvent)
    {
        super(message.getPayload());
        this.message = message;
        this.id = generateEventId();
        this.session = previousEvent.getSession();
        ((MuleSession) session).setComponent(component);
        this.endpoint = endpoint;
        this.synchronous = previousEvent.isSynchronous();
        this.timeout = previousEvent.getTimeout();
        this.outputStream = (ResponseOutputStream) previousEvent.getOutputStream();
        fillProperties(previousEvent);
    }

    public MuleEvent(UMOMessage message, UMOEndpoint endpoint, UMOSession session, boolean synchronous)
    {
        this(message, endpoint, session, synchronous, null);
    }

    /**
     * Contructor.
     * 
     * @param message the event payload
     * @param endpoint the endpoint to associate with the event
     * @param session the previous event if any
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public MuleEvent(UMOMessage message,
                     UMOEndpoint endpoint,
                     UMOSession session,
                     boolean synchronous,
                     ResponseOutputStream outputStream)
    {
        super(message.getPayload());
        this.message = message;
        this.endpoint = endpoint;
        this.session = session;
        this.id = generateEventId();
        this.synchronous = synchronous;
        this.outputStream = outputStream;
        fillProperties(null);
    }

    /**
     * Contructor.
     * 
     * @param message the event payload
     * @param endpoint the endpoint to associate with the event
     * @param session the previous event if any
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public MuleEvent(UMOMessage message, UMOEndpoint endpoint, UMOSession session, String eventId, boolean synchronous)
    {
        super(message.getPayload());
        this.message = message;
        this.endpoint = endpoint;
        this.session = session;
        this.id = eventId;
        this.synchronous = synchronous;
        fillProperties(null);
    }

    /**
     * A helper constructor used to rewrite an event payload
     * 
     * @param message
     * @param rewriteEvent
     */
    public MuleEvent(UMOMessage message, UMOEvent rewriteEvent)
    {
        super(message.getPayload());
        this.message = message;
        this.id = rewriteEvent.getId();
        this.session = rewriteEvent.getSession();
        ((MuleSession) session).setComponent(rewriteEvent.getComponent());
        this.endpoint = rewriteEvent.getEndpoint();
        this.synchronous = rewriteEvent.isSynchronous();
        this.timeout = rewriteEvent.getTimeout();
        this.outputStream = (ResponseOutputStream) rewriteEvent.getOutputStream();
        if (rewriteEvent instanceof MuleEvent) {
            this.transformedMessage = ((MuleEvent) rewriteEvent).getCachedMessage();
        }
        fillProperties(rewriteEvent);
    }

    protected void fillProperties(UMOEvent previousEvent)
    {
        if (previousEvent != null && previousEvent.getProperties() != null) {
            properties.putAll(previousEvent.getProperties());
        }
        if (endpoint != null && endpoint.getProperties() != null) {
            properties.putAll(endpoint.getProperties());
            if (endpoint.getEndpointURI().getParams() != null) {
                properties.putAll(endpoint.getEndpointURI().getParams());
            }
        }

        properties.putAll(message.getProperties());
        //Todo I think the replyTO header lingers when ther is more than one replyTo used in a single request
        //will investigate further after 1.1
//        Map sysProps = PropertiesHelper.getPropertiesWithPrefix(properties, MuleProperties.PROPERTY_PREFIX);
//        for (Iterator iterator = sysProps.keySet().iterator(); iterator.hasNext();) {
//            String s = (String) iterator.next();
//            properties.remove(s);
//        }
        setCredentials();
    }


    protected void setCredentials() {
        if (endpoint.getEndpointURI().getUserInfo() != null) {
            final String userName = endpoint.getEndpointURI().getUsername();
            final String password = endpoint.getEndpointURI().getPassword();
            if (password != null && userName != null)
                credentials = new MuleCredentials(userName, password.toCharArray());
        }
    }

    public UMOCredentials getCredentials() {
        return credentials;
    }
    Object getCachedMessage()
    {
        return transformedMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getPayload()
     */
    public UMOMessage getMessage()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getPayloadAsBytes()
     */
    public byte[] getMessageAsBytes() throws MuleException
    {
        try {
            return message.getPayloadAsBytes();
        } catch (Exception e) {
            throw new MuleException(new Message(Messages.CANT_READ_PAYLOAD_AS_BYTES_TYPE_IS_X, message.getPayload()
                                                                                                      .getClass()
                                                                                                      .getName()), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getTransformedMessage()
     */
    public Object getTransformedMessage() throws TransformerException
    {
        if(isStreaming()) {
            return message.getAdapter();
        }
        if (transformedMessage == null) {
            UMOTransformer tran = endpoint.getTransformer();
            if (tran != null) {
                transformedMessage = tran.transform(message.getPayload());
            } else {
                transformedMessage = message.getPayload();
            }
        }
        return transformedMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getTransformedMessageAsBytes()
     */
    public byte[] getTransformedMessageAsBytes() throws TransformerException
    {
        try {
            return Utility.objectToByteArray(getTransformedMessage());
        } catch (IOException e) {
            throw new TransformerException(new Message(Messages.CANT_READ_PAYLOAD_AS_BYTES_TYPE_IS_X,
                                                       getTransformedMessage().getClass().getName()),
                                           endpoint.getTransformer(),
                                           e);
        }
    }

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into a String. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format
     *         as a Strings.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs
     *             in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public String getTransformedMessageAsString() throws TransformerException
    {
        return getTransformedMessageAsString(getEncoding());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getPayloadAsString()
     */
    public String getMessageAsString() throws UMOException
    {
        return getMessageAsString(getEncoding());
    }

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into a String. The transformer used is the one configured on the
     * endpoint through which this event was received.
     *
     * @param encoding the encoding to use when converting the message to string
     * @return the message transformed into it's recognised or expected format
     *         as a Strings.
     * @throws org.mule.umo.transformer.TransformerException
     *          if a failure occurs in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public String getTransformedMessageAsString(String encoding) throws TransformerException {
        try {
            return new String(getTransformedMessageAsBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new TransformerException(endpoint.getTransformer(), e);
        }
    }

    /**
     * Returns the message contents as a string
     *
     * @param encoding the encoding to use when converting the message to string
     * @return the message contents as a string
     * @throws org.mule.umo.UMOException if the message cannot be converted into a string
     */
    public String getMessageAsString(String encoding) throws UMOException {
        try {
            return message.getPayloadAsString(encoding);
        } catch (Exception e) {
            throw new MuleException(new Message(Messages.CANT_READ_PAYLOAD_AS_STRING_TYPE_IS_X, message.getClass()
                                                                                                       .getName()), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getId()
     */
    public String getId()
    {
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getProperty(java.lang.String)
     */
    public Object getProperty(String name)
    {
        return properties.get(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getProperty(java.lang.String,
     *      java.lang.Object)
     */
    public Object getProperty(String name, Object defaultValue)
    {
        Object prop = getProperty(name);
        return (prop == null ? defaultValue : prop);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#setProperty(java.lang.String,
     *      java.lang.Object)
     */
    public void setProperty(String name, Object value)
    {
        properties.put(name, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getParams()
     */
    public Map getProperties()
    {
        // this is necessary as these properties
        if (message.getCorrelationId() != null) {
            properties.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message.getCorrelationId());
        }
        if (message.getCorrelationGroupSize() != -1) {
            properties.put(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                           new Integer(message.getCorrelationGroupSize()));
        }
        if (message.getCorrelationSequence() != -1) {
            properties.put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                           new Integer(message.getCorrelationSequence()));
        }
        if (message.getReplyTo() != null) {
            properties.put(MuleProperties.MULE_REPLY_TO_PROPERTY, message.getReplyTo());
        }
        return properties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEvent#getEndpoint()
     */
    public UMOEndpoint getEndpoint()
    {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Event: ").append(getId());
        buf.append(", sync=").append(isSynchronous());
        buf.append(", stop processing=").append(isStopFurtherProcessing());
        buf.append(", ").append(endpoint);

        return buf.toString();
    }

    protected String generateEventId()
    {
        return new UUID().getUUID();
    }

    public UMOSession getSession()
    {
        return session;
    }

    void setSession(UMOSession session)
    {
        this.session = session;
    }

    /**
     * Gets the recipient component of this event
     */
    public UMOComponent getComponent()
    {
        return session.getComponent();
    }

    /**
     * Determines whether the default processing for this event will be executed
     * 
     * @return Returns the stopFurtherProcessing.
     */
    public boolean isStopFurtherProcessing()
    {
        return stopFurtherProcessing;
    }

    /**
     * Setting this parameter will stop the Mule framework from processing this
     * event in the standard way. This allow for client code to override default
     * behaviour. The common reasons for doing this are - 1. The UMO has more
     * than one send endpoint configured; the component must dispatch to other
     * prviders programatically by using the component on the current event 2.
     * The UMO doesn't send the current event out through a endpoint. i.e. the
     * processing of the event stops in the uMO.
     * 
     * @param stopFurtherProcessing The stopFurtherProcessing to set.
     */
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        this.stopFurtherProcessing = stopFurtherProcessing;
    }

    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MuleEvent)) {
            return false;
        }

        final MuleEvent event = (MuleEvent) o;

        if (message != null ? !message.equals(event.message) : event.message != null) {
            return false;
        }
        if (!id.equals(event.id)) {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int result;
        result = id.hashCode();
        result = 29 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    public boolean isSynchronous()
    {
        return synchronous;
    }

    public void setSynchronous(boolean value)
    {
        synchronous = value;
    }

    public int getTimeout()
    {
        if(timeout == TIMEOUT_NOT_SET_VALUE) {
            //If this is not set it will use the default timeout value
            timeout = endpoint.getRemoteSyncTimeout();
        }
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return PropertiesHelper.getIntProperty(properties, name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return PropertiesHelper.getLongProperty(properties, name, defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        return PropertiesHelper.getDoubleProperty(properties, name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return PropertiesHelper.getBooleanProperty(properties, name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        properties.put(name, Boolean.valueOf(value));
    }

    public void setIntProperty(String name, int value)
    {
        properties.put(name, new Integer(value));
    }

    public void setLongProperty(String name, long value)
    {
        properties.put(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value)
    {
        properties.put(name, new Double(value));
    }

    /**
     * An outputstream the can optionally be used write response data to an
     * incoming message.
     * 
     * @return an output strem if one has been made available by the message
     *         receiver that received the message
     */
    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    /**
     * Removes a property from the event
     * 
     * @param key the property key to remove
     * @return the removed property or null if the property was not found or if
     *         the underlying message does not return the removed property
     */
    public Object removeProperty(Object key)
    {
        return properties.remove(key);
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        out.writeObject(endpoint.getEndpointURI().toString());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        String uri = (String) in.readObject();
        try {
            endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        } catch (UMOException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Will retrieve a string proerty form the event. If the property does not exist it will be substituted
     * with the default value
     * @param name the name of the proerty to get
     * @param defaultValue the default value to return if the proerty is not set
     * @return the property value or the defaultValue if the proerty is not set
     */
    public String getStringProperty(String name, String defaultValue) {
        return PropertiesHelper.getStringProperty(properties, name, defaultValue);
    }

    public void setStringProperty(String name, String value) {
        setProperty(name, value);
    }

    /**
     * Determines whether the event flow is being streamed
     *
     * @return true if the request should be streamed
     */
    public boolean isStreaming() {
        return endpoint.isStreaming();
    }

    /**
     * Gets the encoding for this message. First it looks to see if encoding has been set on the endpoint, if
     * not it will check the message itself and finally it will fall back to the Mule global configuration for
     * encoding which cannot be null.
     * @return the encoding for the event
     */
    public String getEncoding() {
        String encoding = endpoint.getEndpointEncoding();
        if(encoding==null) {
            encoding = message.getEncoding();
        }
        if(encoding==null) {
            encoding = MuleManager.getConfiguration().getEncoding();
        }
        return encoding;

    }
}
