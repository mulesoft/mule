/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.EventObject;
import java.util.Iterator;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.UUID;

/**
 * <code>MuleEvent</code> represents any data event occuring in the Mule
 * environment. All data sent or received within the Mule environment will be
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
     * Serial version
     */
    private static final long serialVersionUID = 7568207722883309919L;
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    /**
     * The endpoint associated with the event
     */
    private transient UMOImmutableEndpoint endpoint = null;

    /**
     * the Universally Unique ID for the event
     */
    private String id = null;

    /**
     * The payload message used to read the payload of the event
     */
    private UMOMessage message = null;

    private transient UMOSession session;

    private boolean stopFurtherProcessing = false;

    private boolean synchronous = false;

    private int timeout = TIMEOUT_NOT_SET_VALUE;

    private transient ResponseOutputStream outputStream = null;

    private transient Object transformedMessage = null;

    private UMOCredentials credentials = null;

    protected String[] ignoredPropertyOverrides = new String[]{"method"};

    /**
     * Properties cache that only reads properties once from the inbound message
     * and merges them with any properties on the endpoint. The message
     * properties take precedence over the endpoint properties
     */
    public MuleEvent(UMOMessage message,
                     UMOImmutableEndpoint endpoint,
                     UMOComponent component,
                     UMOEvent previousEvent)
    {
        super(message.getPayload());
        this.message = message;
        this.id = generateEventId();
        this.session = previousEvent.getSession();
        ((MuleSession)session).setComponent(component);
        this.endpoint = endpoint;
        this.synchronous = previousEvent.isSynchronous();
        this.timeout = previousEvent.getTimeout();
        this.outputStream = (ResponseOutputStream)previousEvent.getOutputStream();
        fillProperties(previousEvent);
    }

    public MuleEvent(UMOMessage message, UMOImmutableEndpoint endpoint, UMOSession session, boolean synchronous)
    {
        this(message, endpoint, session, synchronous, null);
    }

    /**
     * Contructor.
     *
     * @param message
     *            the event payload
     * @param endpoint
     *            the endpoint to associate with the event
     * @param session
     *            the previous event if any
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public MuleEvent(UMOMessage message,
                     UMOImmutableEndpoint endpoint,
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
     * @param message
     *            the event payload
     * @param endpoint
     *            the endpoint to associate with the event
     * @param session
     *            the previous event if any
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public MuleEvent(UMOMessage message,
                     UMOImmutableEndpoint endpoint,
                     UMOSession session,
                     String eventId,
                     boolean synchronous)
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
        ((MuleSession)session).setComponent(rewriteEvent.getComponent());
        this.endpoint = rewriteEvent.getEndpoint();
        this.synchronous = rewriteEvent.isSynchronous();
        this.timeout = rewriteEvent.getTimeout();
        this.outputStream = (ResponseOutputStream)rewriteEvent.getOutputStream();
        if (rewriteEvent instanceof MuleEvent) {
            this.transformedMessage = ((MuleEvent)rewriteEvent).getCachedMessage();
        }
        fillProperties(rewriteEvent);
    }

    protected void fillProperties(UMOEvent previousEvent)
    {
        if (previousEvent != null) {
            UMOMessage msg = previousEvent.getMessage();
            synchronized (msg) {
                for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
                    String prop = (String)iterator.next();
                    Object value = msg.getProperty(prop);
                    // don't overwrite property on the message
                    if (!ignoreProperty(prop, value)) {
                        message.setProperty(prop, value);
                    }

                    if(logger.isDebugEnabled()) {
                        Object currentValue = message.getProperty(prop);
                        if(!value.equals(currentValue)) {
                            logger.warn("Property on the current message " + prop +"=" + currentValue +
                                " overrides property on the previous event: " + prop +"=" + value);
                        }
                    }
                }
            }
        }

        if (endpoint != null && endpoint.getProperties() != null) {
            for (Iterator iterator = endpoint.getProperties().keySet().iterator(); iterator.hasNext();) {
                String prop = (String)iterator.next();
                Object value = endpoint.getProperties().get(prop);
                // don't overwrite property on the message
                if (!ignoreProperty(prop, value)) {
                    message.setProperty(prop, value);
                }

                if(logger.isDebugEnabled()) {
                    Object currentValue = message.getProperty(prop);
                    if(!value.equals(currentValue)) {
                        logger.warn("Property on the current message " + prop +"=" + currentValue +
                            " overrides property on the endpoint: " + prop +"=" + value);
                    }
                }
            }
        }

        setCredentials();
    }

    // TODO this method is pretty confusing and could need some documentation.
    // value is not used at all, instead the value for the passed key is looked up twice.
    protected boolean ignoreProperty(String key, Object value) {
        if (key == null || value == null) {
            return true;
        }

        if (key.startsWith(MuleProperties.PROPERTY_PREFIX) && message.getProperty(key) != null) {
            return true;
        }

        for (int i = 0; i < ignoredPropertyOverrides.length; i++) {
            if(key.equals(ignoredPropertyOverrides[i])) {
                return false;
            }
        }

        if (message.getProperty(key) != null) {
            return true;
        }

        return false;
    }

    protected void setCredentials()
    {
        if (endpoint.getEndpointURI().getUserInfo() != null) {
            final String userName = endpoint.getEndpointURI().getUsername();
            final String password = endpoint.getEndpointURI().getPassword();
            if (password != null && userName != null) {
                credentials = new MuleCredentials(userName, password.toCharArray());
            }
        }
    }

    public UMOCredentials getCredentials()
    {
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
        }
        catch (Exception e) {
            throw new MuleException(new Message(Messages.CANT_READ_PAYLOAD_AS_BYTES_TYPE_IS_X, message
                    .getPayload().getClass().getName()), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOEvent#getTransformedMessage()
     */
    public Object getTransformedMessage() throws TransformerException
    {
        if (isStreaming()) {
            return message.getAdapter();
        }
        if (transformedMessage == null) {
            UMOTransformer tran = endpoint.getTransformer();
            if (tran != null) {
                transformedMessage = tran.transform(message.getPayload());
            }
            else {
                transformedMessage = message.getPayload();
            }
        }
        return transformedMessage;
    }

    /**
     * This method will attempt to convert the transformed message into an array
     * of bytes It will first check if the result of the transformation is a
     * byte array and return that. Otherwise if the the result is a string it
     * will serialized the CONTENTS of the string not the String object. finally
     * it will check if the result is a Serializable object and convert that to
     * an array of bytes.
     *
     * @return a byte[] representation of the message
     * @throws TransformerException
     *             if an unsupported encoding is being used or if the result
     *             message is not a String byte[] or Seializable object
     */
    public byte[] getTransformedMessageAsBytes() throws TransformerException
    {
        Object msg = getTransformedMessage();
        if (msg instanceof byte[]) {
            return (byte[])msg;
        }
        else if (msg instanceof String) {
            try {
                return msg.toString().getBytes(getEncoding());
            }
            catch (UnsupportedEncodingException e) {
                throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X, msg
                        .getClass().getName(), e));
            }
        }
        else if (msg instanceof Serializable) {
            try {
                return SerializationUtils.serialize((Serializable)msg);
            }
            catch (Exception e) {
                throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, msg
                        .getClass().getName(), "byte[]"), e);
            }
        }
        else {
            throw new TransformerException(new Message(Messages.TRANSFORM_ON_X_NOT_OF_SPECIFIED_TYPE_X,
                    msg.getClass().getName(), "byte[] or " + Serializable.class.getName()));
        }
    }

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into a String. The transformer used is the one configured on the
     * endpoint through which this event was received.
     *
     * @return the message transformed into it's recognised or expected format
     *         as a Strings.
     * @throws org.mule.umo.transformer.TransformerException
     *             if a failure occurs in the transformer
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
     * @param encoding
     *            the encoding to use when converting the message to string
     * @return the message transformed into it's recognised or expected format
     *         as a Strings.
     * @throws org.mule.umo.transformer.TransformerException
     *             if a failure occurs in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public String getTransformedMessageAsString(String encoding) throws TransformerException
    {
        try {
            return new String(getTransformedMessageAsBytes(), encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new TransformerException(endpoint.getTransformer(), e);
        }
    }

    /**
     * Returns the message contents as a string
     *
     * @param encoding
     *            the encoding to use when converting the message to string
     * @return the message contents as a string
     * @throws org.mule.umo.UMOException
     *             if the message cannot be converted into a string
     */
    public String getMessageAsString(String encoding) throws UMOException
    {
        try {
            return message.getPayloadAsString(encoding);
        }
        catch (Exception e) {
            throw new MuleException(new Message(Messages.CANT_READ_PAYLOAD_AS_STRING_TYPE_IS_X, message
                    .getClass().getName()), e);
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

    /**
     *
     * @param name
     * @return
     */
    public Object getProperty(String name)
    {
        return message.getProperty(name);
    }

    /**
     *
     * @see org.mule.umo.UMOEvent#getProperty(java.lang.String, boolean)
     */
    public Object getProperty(String name, boolean exhaustiveSearch) {
        return getProperty(name, /*defaultValue*/null, exhaustiveSearch);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOEvent#getProperty(java.lang.String,
     *      java.lang.Object)
     * @param name
     * @param defaultValue
     * @return
     */
    public Object getProperty(String name, Object defaultValue)
    {
        return message.getProperty(name, defaultValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOEvent#getProperty(java.lang.String, java.lang.Object, boolean)
     */
    public Object getProperty(String name, Object defaultValue, boolean exhaustiveSearch) {
        Object property = getProperty(name);

        if (exhaustiveSearch) {
            // Search the endpoint
            if (property == null) {
                property = MapUtils.getObject(getEndpoint().getEndpointURI().getParams(), name, null);
            }

            // Search the connector
            if (property == null) {
                try {
                    property = PropertyUtils.getProperty(getEndpoint().getConnector(), name);
                } catch (Exception e) {
                    // Ignore this exception, it just means that the connector has no such property.
                }
            }
        }
        return (property == null ? defaultValue : property);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOEvent#setProperty(java.lang.String,
     *      java.lang.Object)
     */
    public void setProperty(String name, Object value)
    {
        message.setProperty(name, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOEvent#getEndpoint()
     */
    public UMOImmutableEndpoint getEndpoint()
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
        StringBuffer buf = new StringBuffer(64);
        buf.append("Event: ").append(getId());
        buf.append(", sync=").append(isSynchronous());
        buf.append(", stop processing=").append(isStopFurtherProcessing());
        buf.append(", ").append(endpoint);

        return buf.toString();
    }

    protected String generateEventId()
    {
        return UUID.getUUID();
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
     * @param stopFurtherProcessing
     *            The stopFurtherProcessing to set.
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

        final MuleEvent event = (MuleEvent)o;

        if (message != null ? !message.equals(event.message) : event.message != null) {
            return false;
        }
        return id.equals(event.id);
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
        if (timeout == TIMEOUT_NOT_SET_VALUE) {
            // If this is not set it will use the default timeout value
            timeout = endpoint.getRemoteSyncTimeout();
        }
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Gets an int property on the nessage
     *
     * @param name
     */
    public int getIntProperty(String name, int defaultValue)
    {
        return message.getIntProperty(name, defaultValue);
    }

    /**
     * Gets a long property on the nessage
     *
     * @param name
     */
    public long getLongProperty(String name, long defaultValue)
    {
        return message.getLongProperty(name, defaultValue);
    }

    /**
     * Gets a double property on the nessage
     *
     * @param name
     */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return message.getDoubleProperty(name, defaultValue);
    }

    /**
     * Gets a boolean property on the nessage
     *
     * @param name
     */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return message.getBooleanProperty(name, defaultValue);
    }

    /**
     * Sets a boolean property on the nessage
     *
     * @param name
     * @param value
     */
    public void setBooleanProperty(String name, boolean value)
    {
        message.setBooleanProperty(name, value);
    }

    /**
     * Sets an int property on the nessage
     *
     * @param name
     * @param value
     */
    public void setIntProperty(String name, int value)
    {
        message.setIntProperty(name, value);
    }

    /**
     * Sets a long property on the nessage
     *
     * @param name
     * @param value
     */
    public void setLongProperty(String name, long value)
    {
        message.setLongProperty(name, value);
    }

    /**
     * Sets a double property on the nessage
     *
     * @param name
     * @param value
     */
    public void setDoubleProperty(String name, double value)
    {
        message.setDoubleProperty(name, value);
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
     * @param key
     *            the property key to remove
     * @return the removed property or null if the property was not found or if
     *         the underlying message does not return the removed property
     */
    public Object removeProperty(String key)
    {
        return message.removeProperty(key);
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        out.writeObject(endpoint.getEndpointURI().toString());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        String uri = (String)in.readObject();
        try {
            endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        catch (UMOException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }

    /**
     * Will retrieve a string proerty form the event. If the property does not
     * exist it will be substituted with the default value
     *
     * @param name
     *            the name of the proerty to get
     * @param defaultValue
     *            the default value to return if the proerty is not set
     * @return the property value or the defaultValue if the proerty is not set
     */
    public String getStringProperty(String name, String defaultValue)
    {
        return message.getStringProperty(name, defaultValue);
    }

    public void setStringProperty(String name, String value)
    {
        setProperty(name, value);
    }

    /**
     * Determines whether the event flow is being streamed
     *
     * @return true if the request should be streamed
     */
    public boolean isStreaming()
    {
        return endpoint.isStreaming();
    }

    /**
     * Gets the encoding for this message. First it looks to see if encoding has
     * been set on the endpoint, if not it will check the message itself and
     * finally it will fall back to the Mule global configuration for encoding
     * which cannot be null.
     *
     * @return the encoding for the event
     */
    public String getEncoding()
    {
        String encoding = endpoint.getEncoding();
        if (encoding == null) {
            encoding = message.getEncoding();
        }
        if (encoding == null) {
            encoding = MuleManager.getConfiguration().getEncoding();
        }
        return encoding;
    }

}
