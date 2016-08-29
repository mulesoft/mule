/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.agent;

import static org.mule.runtime.core.DefaultMessageContext.create;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.agent.AbstractNotificationLoggerAgent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.exception.MessagingExceptionHandlerToSystemAdapter;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>EndpointAbstractEventLoggerAgent</code> will forward server notifications to a configurered endpoint uri.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class EndpointNotificationLoggerAgent extends AbstractNotificationLoggerAgent {

  private boolean ignoreEndpointMessageNotifications = false;

  private OutboundEndpoint endpoint = null;
  private List<Integer> ignoredNotifications = new ArrayList<>();

  public EndpointNotificationLoggerAgent() {
    super("Endpoint Logger Agent");
    // List of notifications to ignore, because when these notifications are
    // received the notification endpoint is no longer available
    ignoredNotifications.add(MuleContextNotification.CONTEXT_STOPPED);
    ignoredNotifications.add(MuleContextNotification.CONTEXT_DISPOSING);
    ignoredNotifications.add(MuleContextNotification.CONTEXT_DISPOSED);
  }

  public boolean isIgnoreEndpointMessageNotifications() {
    return ignoreEndpointMessageNotifications;
  }

  public void setIgnoreEndpointMessageNotifications(boolean ignoreEndpointMessageNotifications) {
    this.ignoreEndpointMessageNotifications = ignoreEndpointMessageNotifications;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    // first see if we're logging notifications to an endpoint
    try {
      if (endpoint == null) {
        throw new InitialisationException(CoreMessages.propertiesNotSet("endpoint"), this);
      }
      if (endpoint instanceof MuleContextAware) {
        ((MuleContextAware) endpoint).setMuleContext(muleContext);
      }
      if (endpoint instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) endpoint)
            .setMessagingExceptionHandler(new MessagingExceptionHandlerToSystemAdapter(muleContext));
      }
      if (endpoint instanceof Initialisable) {
        ((Initialisable) endpoint).initialise();
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    if (!isIgnoreMessageNotifications() && !ignoreEndpointMessageNotifications) {
      ServerNotificationListener<EndpointMessageNotification> l = notification -> logEvent(notification);
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }
  }

  @Override
  protected void logEvent(ServerNotification e) {
    if (endpoint != null && !ignoredNotifications.contains(new Integer(e.getAction()))) {
      if (!endpoint.getConnector().isStarted()) {
        logger.warn("Endpoint not started: " + endpoint.getEndpointURI() + ". Cannot dispatch notification: " + e);
        return;
      }
      if ((e.getAction() == ConnectionNotification.CONNECTION_FAILED
          || e.getAction() == ConnectionNotification.CONNECTION_DISCONNECTED)
          && (e.getSource()).equals(endpoint.getConnector())) {
        // If this is a CONNECTION_FAILED or
        // CONNECTION_DISCONNECTED notification for the same connector that
        // is being used for notifications then ignore.
        return;
      }
      MuleMessage msg = MuleMessage.builder().payload(e).build();
      try {
        // TODO: Filters should really be applied by the endpoint
        if (endpoint.getFilter() != null && !endpoint.getFilter().accept(msg)) {
          if (logger.isInfoEnabled()) {
            logger.info("Message not accepted with filter: " + endpoint.getFilter());
          }
          return;
        }

        FlowConstruct flowConstruct = new FlowConstruct() {

          @Override
          public MuleContext getMuleContext() {
            return muleContext;
          }

          @Override
          public String getName() {
            return "EndpointNotificationLoggerAgent";
          }

          @Override
          public LifecycleState getLifecycleState() {
            return null;
          }

          @Override
          public MessagingExceptionHandler getExceptionListener() {
            return null;
          }

          @Override
          public FlowConstructStatistics getStatistics() {
            return null;
          }
        };

        MuleEvent event = MuleEvent.builder(create(flowConstruct, "EndpointNotificationLoggerAgent")).message(msg)
            .exchangePattern(endpoint.getExchangePattern()).flow(flowConstruct).build();
        event.setEnableNotifications(false);
        endpoint.process(event);
      } catch (Exception e1) {
        // TODO MULE-863: If this is an error, do something better than this
        logger
            .error("Failed to dispatch event: " + e.toString() + " over endpoint: " + endpoint + ". Error is: " + e1.getMessage(),
                   e1);
      }
    }
  }

  /**
   * Should be a 1 line description of the agent
   */
  @Override
  public String getDescription() {
    StringBuilder buf = new StringBuilder();
    buf.append(getName()).append(": ");
    if (endpoint != null) {
      buf.append("Forwarding notifications to: ").append(endpoint.getEndpointURI().getAddress());
    }
    return buf.toString();
  }

  public OutboundEndpoint getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }
}
