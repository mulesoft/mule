/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.service;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NameableObject;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.model.Model;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.source.MessageSource;
import org.mule.management.stats.ServiceStatistics;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;

import java.io.Serializable;

/**
 * <code>Service</code> is the internal representation of a Mule Managed service. It
 * is responsible for managing the interaction of events to and from the service as
 * well as managing pooled resources.
 */
@Deprecated
public interface Service extends Serializable, FlowConstruct, Lifecycle, NameableObject
{
    /**
     * Makes an asynchronous event call to the service.
     *
     * @param event the event to consume
     * @throws MuleException if the event fails to be processed
     * @deprecated
     */
    @Deprecated
    void dispatchEvent(MuleEvent event) throws MuleException;

    /**
     * Makes a synchronous event call to the service. This event will be consumed by
     * the service and a result returned.
     *
     * @param event the event to consume
     * @return a MuleMessage containing the resulting message and properties
     * @throws MuleException if the event fails to be processed
     * @deprecated
     */
    @Deprecated
    MuleEvent sendEvent(MuleEvent event) throws MuleException;

    /**
     * Determines whether this service has been started
     *
     * @return true is the service is started and ready to receive events
     */
    boolean isStarted();

    boolean isStopped();

    /**
     * Pauses event processing for a single Mule Service. Unlike <code>stop()</code>, a paused
     * service will still consume messages from the underlying transport, but those
     * messages will be queued until the service is resumed.
     */
    void pause() throws MuleException;

    /**
     * Resumes a single Mule Service that has been paused. If the service is not
     * paused nothing is executed.
     */
    void resume() throws MuleException;

    /**
     * @return true if the service is in a paused state, false otherwise
     */
    boolean isPaused();

    MessageSource getMessageSource();

    /**
     * Outbound Routers control how events are published by a service once. the
     * event has been processed. If no router is set. A default will be used that
     * uses the outboundProvider set on his descriptor to route the event.
     *
     * @return the outbound router for this service
     * @see OutboundRouterCollection
     */
    MessageProcessor getOutboundMessageProcessor();

    /**
     * Returns the initial state of this service
     *
     * @return the initial state of this service
     */
    String getInitialState();

    /**
     * Returns the name of the model that this descriptor is registered with.
     * @return the name of the model that this descriptor is registered with or null
     *         if this descriptor has not been registered with a model yet
     */
    Model getModel();

    void setMessageSource(MessageSource messageSource);

    /**
     * Outbound message processor controls how events are published by a service once the
     * event has been processed. If no router is set a default will be used that
     * uses the outboundProvider set on his descriptor to route the event.
     */
    void setOutboundMessageProcessor(MessageProcessor processor);

    /**
     * Sets the initial state of this service
     *
     * @param state the initial state of this service
     */
    void setInitialState(String state);

    void setModel(Model model);

    /**
     * Returns the Component that is a invoked by a {@link Service} for each incoming
     * {@link MuleEvent} routed on by the inbound routers.
     */
    Component getComponent();

    /**
     * Sets the Component that is a invoked by a {@link Service} for each incoming
     * {@link MuleEvent} routed on by the inbound routers.
     */
    void setComponent(Component component);

    /**
     * Returns the Service statistics.  This provides Service router and component statistics.
     */
    ServiceStatistics getStatistics();

    MuleContext getMuleContext();

    LifecycleManager getLifecycleManager();

    void setAsyncReplyMessageSource(ServiceAsyncReplyCompositeMessageSource asyncReplyMessageSource);

    ServiceAsyncReplyCompositeMessageSource getAsyncReplyMessageSource();

    MessagingExceptionHandler getExceptionListener();

    void setExceptionListener(MessagingExceptionHandler exceptionListener);
}
