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
package org.mule.impl;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.transformer.TransformerException;

import java.io.OutputStream;
import java.util.Map;

/**
 * <code>MuleEventContext</code> is the context object for the current
 * request. Using the context, developers can send/dispatch/receive events
 * programmatically as well as manage transactions.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleEventContext implements UMOEventContext
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleEventContext.class);

    private UMOEvent event;
    private UMOSession session;

    MuleEventContext(UMOEvent event)
    {
        this.event = event;
        this.session = event.getSession();
    }

    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    public UMOMessage getMessage()
    {
        return event.getMessage();
    }

    /**
     * Reterns the conents of the message as a byte array.
     * 
     * @return the conents of the message as a byte array
     * @throws org.mule.umo.UMOException if the message cannot be converted into
     *             an array of bytes
     */
    public byte[] getMessageAsBytes() throws UMOException
    {
        return event.getMessageAsBytes();
    }

    /**
     * Returns the message transformed into it's recognised or expected format.
     * The transformer used is the one configured on the endpoint through which
     * this event was received.
     * 
     * @return the message transformed into it's recognised or expected format.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs
     *             in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public Object getTransformedMessage() throws TransformerException
    {
        return event.getTransformedMessage();
    }

    /**
     * Returns the message transformed into it's recognised or expected format.
     * The transformer used is the one configured on the endpoint through which
     * this event was received.
     *
     * @param expectedType The class type required for the return object.  This param
     *                     just provides a convienient way to manage type casting of transformed objects
     * @return the message transformed into it's recognised or expected format.
     * @throws org.mule.umo.transformer.TransformerException
     *          if a failure occurs or if
     *          the return type is not the same as the expected type
     *          in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public Object getTransformedMessage(Class expectedType) throws TransformerException {
        Object message = getTransformedMessage();
        if(expectedType!=null && expectedType.isAssignableFrom(message.getClass())) {
            return message;
        } else {
            throw new TransformerException(new Message(Messages.TRANSFORM_ON_X_NOT_OF_SPECIFIED_TYPE_X,
                    this.getComponentDescriptor().getName(), expectedType), this.event.getEndpoint().getTransformer());
        }
    }

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into an array of bytes. The transformer used is the one
     * configured on the endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format
     *         as an array of bytes.
     * @throws org.mule.umo.transformer.TransformerException if a failure occurs
     *             in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    public byte[] getTransformedMessageAsBytes() throws TransformerException
    {
        return event.getTransformedMessageAsBytes();
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
        return event.getTransformedMessageAsString();
    }

    /**
     * Returns the message contents as a string
     * 
     * @return the message contents as a string
     * @throws org.mule.umo.UMOException if the message cannot be converted into
     *             a string
     */
    public String getMessageAsString() throws UMOException
    {
        return event.getMessageAsString();
    }

    /**
     * Returns the current transaction (if any) for the session
     * 
     * @return the current transaction for the session or null if there is no
     *         transaction in progress
     */
    public UMOTransaction getCurrentTransaction()
    {
        return TransactionCoordination.getInstance().getTransaction();
    }

    public void markTransactionForRollback() throws TransactionException
    {
        if (getCurrentTransaction() != null) {
            getCurrentTransaction().setRollbackOnly();
        }
    }

    /**
     * This will send an event via the configured outbound router on the
     * component
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws org.mule.umo.UMOException if there is no outbound endpoint
     *             configured on the component or the events fails during
     *             dispatch
     */
    public UMOMessage sendEvent(Object message) throws UMOException
    {
        return sendEvent(new MuleMessage(message, event.getProperties()));
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the
     * endpoint configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through.
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        // If synchronous receive has not been explicitly set, default it to
        // true
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the
     * endpoint configured for the event
     * 
     * @param message the message payload to send
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message) throws UMOException
    {
        // If synchronous receive has not been explicitly set, default it to
        // true
        setRemoteSync(message, event.getEndpoint());
        return session.sendEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri The endpointUri to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpointUri
     */
    public UMOMessage sendEvent(UMOMessage message, UMOEndpointURI endpointUri) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);

        // If synchronous receive has not been explicitly set, default it to
        // true
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * sends an event request via the configured outbound router for this
     * component. This method return immediately, but the result of the event
     * invocation available from the returned a Future result that can be
     * accessed later by the the returned FutureMessageResult. the Future
     * messageResult can be queried at any time to check that the invocation has
     * completed. A timeout is associated with the invocation, which is the
     * maximum time in milli-seconds that the invocation should take to complete
     * 
     * @param message the object that is the payload of the event
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *             or transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final Object message, final int timeout) throws UMOException
    {
        FutureMessageResult result = new FutureMessageResult();

        Callable callable = new Callable() {
            public Object call() throws Exception
            {
                UMOMessage umoMessage = new MuleMessage(message, event.getProperties());
                umoMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                umoMessage.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(umoMessage);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this
     * component. This method return immediately, but the result of the event
     * invocation available from the returned a Future result that can be
     * accessed later by the the returned FutureMessageResult. the Future
     * messageResult can be queried at any time to check that the invocation has
     * completed. A timeout is associated with the invocation, which is the
     * maximum time in milli-seconds that the invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *             or transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message, final int timeout) throws UMOException
    {
        FutureMessageResult result = new FutureMessageResult();

        Callable callable = new Callable() {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this
     * component. This method return immediately, but the result of the event
     * invocation available from the returned a Future result that can be
     * accessed later by the the returned FutureMessageResult. the Future
     * messageResult can be queried at any time to check that the invocation has
     * completed. A timeout is associated with the invocation, which is the
     * maximum time in milli-seconds that the invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param endpointUri the endpointUri to dispatch to
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *             or transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message,
                                              final UMOEndpointURI endpointUri,
                                              final int timeout) throws UMOException
    {
        FutureMessageResult result = new FutureMessageResult();

        Callable callable = new Callable() {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message, endpointUri);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * sends an event request via the configured outbound router for this
     * component. This method return immediately, but the result of the event
     * invocation available from the returned a Future result that can be
     * accessed later by the the returned FutureMessageResult. the Future
     * messageResult can be queried at any time to check that the invocation has
     * completed. A timeout is associated with the invocation, which is the
     * maximum time in milli-seconds that the invocation should take to complete
     * 
     * @param message the UMOMessage of the event
     * @param endpointName The endpoint name to disptch the event through. This
     *            will be looked up first on the component configuration and
     *            then on the mule manager configuration
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *             or transfromers cannot be found
     * @see org.mule.umo.FutureMessageResult
     */
    public FutureMessageResult sendEventAsync(final UMOMessage message, final String endpointName, final int timeout)
            throws UMOException
    {
        FutureMessageResult result = new FutureMessageResult();

        Callable callable = new Callable() {
            public Object call() throws Exception
            {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
                message.setIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, timeout);
                return sendEvent(message, endpointName);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the
     * endpoint configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This
     *            will be looked up first on the component configuration and
     *            then on the mule manager configuration
     * @return the return Message from the call or null if there was no result
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpoint
     */
    public UMOMessage sendEvent(UMOMessage message, String endpointName) throws UMOException
    {
        UMOEndpoint endpoint = MuleManager.getInstance().lookupEndpoint(endpointName);
        setRemoteSync(message, endpoint);
        return session.sendEvent(message, endpoint);
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the component for this session
     * 
     * @param message payload to dispatch
     * @throws org.mule.umo.UMOException if there is no outbound endpoint
     *             configured on the component or the events fails during
     *             dispatch
     */
    public void dispatchEvent(Object message) throws UMOException
    {
        session.dispatchEvent(new MuleMessage(message, event.getProperties()));
    }

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the component for this session
     * 
     * @param message the message to send
     * @throws org.mule.umo.UMOException if there is no outbound endpoint
     *             configured on the component or the events fails during
     *             dispatch
     */
    public void dispatchEvent(UMOMessage message) throws UMOException
    {
        session.dispatchEvent(message);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the
     * endpointUri configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointUri the endpointUri to dispatc the event to first on the
     *            component configuration and then on the mule manager
     *            configuration
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpointUri
     */
    public void dispatchEvent(UMOMessage message, UMOEndpointURI endpointUri) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the
     * endpoint configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This
     *            will be looked up first on the component configuration and
     *            then on the mule manager configuration
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpoint
     */
    public void dispatchEvent(UMOMessage message, String endpointName) throws UMOException
    {
        session.dispatchEvent(message, endpointName);
    }

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the
     * endpoint configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through.
     * @throws org.mule.umo.UMOException if the event fails to be processed by
     *             the component or the transport for the endpoint
     */
    public void dispatchEvent(UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        session.dispatchEvent(message, endpoint);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpoint the endpoint identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(UMOEndpoint endpoint, long timeout) throws UMOException
    {
        return session.receiveEvent(endpoint, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpointName the endpoint identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(String endpointName, long timeout) throws UMOException
    {
        return session.receiveEvent(endpointName, timeout);
    }

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpointUri the endpointUri on which the event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws org.mule.umo.UMOException if the request operation fails
     */
    public UMOMessage receiveEvent(UMOEndpointURI endpointUri, long timeout) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        return session.receiveEvent(endpoint, timeout);
    }

    /**
     * 
     * @return
     */
    public UMODescriptor getComponentDescriptor()
    {
        return event.getComponent().getDescriptor();
    }

    /**
     * Gets a property associated with the current event. Calling this method is
     * equivilent to calling <code>event.getMessage().getProperty(...)</code>
     * 
     * @param name the property name
     * @return the property value or null if the property does not exist
     */
    public Object getProperty(String name)
    {
        return event.getProperty(name);
    }

    /**
     * Gets a property associated with the current event. Calling this method is
     * equivilent to calling
     * <code>event.getMessage().getProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public Object getProperty(String name, Object defaultValue)
    {
        return event.getProperty(name, defaultValue);
    }

    /**
     * Gets an Integer property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getIntProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public int getIntProperty(String name, int defaultValue)
    {
        return event.getIntProperty(name, defaultValue);
    }

    /**
     * Gets a Long property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getLongProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public long getLongProperty(String name, long defaultValue)
    {
        return event.getLongProperty(name, defaultValue);
    }

    /**
     * Gets a Double property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getDoubleProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return event.getDoubleProperty(name, defaultValue);
    }

    /**
     * Gets a Boolean property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getbooleanProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return event.getBooleanProperty(name, defaultValue);
    }

    /**
     * Sets a property associated with the current event. Calling this method is
     * equivilent to calling
     * <code>event.getMessage().setProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setProperty(String name, Object value)
    {
        event.setProperty(name, value);
    }

    /**
     * Sets a Boolean property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setBooleanProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setBooleanProperty(String name, boolean value)
    {
        event.setBooleanProperty(name, value);
    }

    /**
     * Sets an Integer property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setIntProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setIntProperty(String name, int value)
    {
        event.setIntProperty(name, value);
    }

    /**
     * Sets a Long property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setLongProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setLongProperty(String name, long value)
    {
        event.setLongProperty(name, value);
    }

    /**
     * Sets a Double property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setDoubleProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setDoubleProperty(String name, double value)
    {
        event.setDoubleProperty(name, value);
    }

    /**
     * Returns a map of properties associated with the event
     * 
     * @return a map of properties on the event
     */
    public Map getProperties()
    {
        return event.getProperties();
    }

    /**
     * Determines whether the default processing for this event will be
     * executed. By default, the Mule server will route events according to a
     * components configuration. The user can override this behaviour by
     * obtaining a reference to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the UMOEventContext
     * for the current thread. The user can programmatically control how events
     * are dispached.
     * 
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.umo.manager.UMOManager
     * @see org.mule.umo.UMOEventContext
     * @see org.mule.umo.lifecycle.Callable
     */
    public boolean isStopFurtherProcessing()
    {
        return event.isStopFurtherProcessing();
    }

    /**
     * Determines whether the default processing for this event will be
     * executed. By default, the Mule server will route events according to a
     * components configuration. The user can override this behaviour by
     * obtaining a reference to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the UMOEventContext
     * for the current thread. The user can programmatically control how events
     * are dispached.
     * 
     * @param stopFurtherProcessing the value to set.
     */
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        event.setStopFurtherProcessing(stopFurtherProcessing);
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
        return event.getOutputStream();
    }

    /**
     * Determines whether the was sent synchrounously or not
     * 
     * @return true if the event is synchronous
     */
    public boolean isSynchronous()
    {
        return event.isSynchronous();
    }

    public UMOEndpointURI getEndpointURI()
    {
        return event.getEndpoint().getEndpointURI();
    }

    /**
     * Returns the transaction for the current event or null if there is no
     * transaction in progresss
     * 
     * @return the transaction for the current event or null if there is no
     *         transaction in progresss
     */
    public UMOTransaction getTransaction()
    {
        return TransactionCoordination.getInstance().getTransaction();
    }

    /**
     * Get the timeout value associated with the event
     *
     * @return the timeout for the event
     */
    public int getTimeout() {
        return event.getTimeout();
    }

    private void setRemoteSync(UMOMessage message, UMOEndpoint endpoint) {
        if(endpoint.isRemoteSync()) {
            if (getTransaction() == null) {
                message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
            } else {
                throw new IllegalStateException(new Message(Messages.CANNOT_USE_TX_AND_REMOTE_SYNC).getMessage());
            }
        }
    }
}
