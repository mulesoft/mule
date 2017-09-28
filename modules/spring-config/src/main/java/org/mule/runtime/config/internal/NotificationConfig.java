/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.notification.AsyncMessageNotification;
import org.mule.runtime.api.notification.AsyncMessageNotificationListener;
import org.mule.runtime.api.notification.ClusterNodeNotificationListener;
import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.api.notification.ConnectionNotificationListener;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.ManagementNotification;
import org.mule.runtime.api.notification.ManagementNotificationListener;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.RoutingNotification;
import org.mule.runtime.api.notification.RoutingNotificationListener;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Notification helper object for creating notification manager configuration.
 * <p>
 * For now we create specific types {@code Notification.EnabledNotification} and {@code Notification.DisabledNotification} for
 * elements that have children of the same type but that must be injected in different collections of the parent object. Ideally
 * this should not be needed or we don't have to support such use case anymore but for previous elements coming from previous mule
 * version we have to keep the XML syntax as it is.
 *
 * @since 4.0
 */
public abstract class NotificationConfig<N extends Notification, L extends NotificationListener<N>>
    extends AbstractComponent {

  public static final Map<String, Class<? extends Notification>> EVENT_MAP;
  public static final Map<String, Class<? extends NotificationListener>> INTERFACE_MAP;

  static {
    EVENT_MAP = ImmutableMap.<String, Class<? extends Notification>>builder()
        .put("CONTEXT", MuleContextNotification.class)
        .put("SECURITY", SecurityNotification.class)
        .put("CONNECTOR-MESSAGE", ConnectorMessageNotification.class)
        .put("MANAGEMENT", ManagementNotification.class)
        .put("MESSAGE-PROCESSOR", MessageProcessorNotification.class)
        .put("EXCEPTION-STRATEGY", ErrorHandlerNotification.class)
        .put("CONNECTION", ConnectionNotification.class)
        .put("CUSTOM", CustomNotification.class)
        .put("EXCEPTION", ExceptionNotification.class)
        .put("TRANSACTION", TransactionNotification.class)
        .put("ROUTING", RoutingNotification.class)
        .put("PIPELINE-MESSAGE", PipelineMessageNotification.class)
        .put("ASYNC-MESSAGE", AsyncMessageNotification.class)
        .build();

    INTERFACE_MAP = ImmutableMap.<String, Class<? extends NotificationListener>>builder()
        .put("CONTEXT", MuleContextNotificationListener.class)
        .put("SECURITY", SecurityNotificationListener.class)
        .put("MANAGEMENT", ManagementNotificationListener.class)
        .put("MESSAGE-PROCESSOR", MessageProcessorNotificationListener.class)
        .put("EXCEPTION-STRATEGY", ErrorHandlerNotificationListener.class)
        .put("CONNECTION", ConnectionNotificationListener.class)
        .put("CUSTOM", CustomNotificationListener.class)
        .put("CONNECTOR-MESSAGE", ConnectorMessageNotificationListener.class)
        .put("EXCEPTION", ExceptionNotificationListener.class)
        .put("TRANSACTION", TransactionNotificationListener.class)
        .put("ROUTING", RoutingNotificationListener.class)
        .put("CLUSTER-NODE", ClusterNodeNotificationListener.class)
        .put("PIPELINE-MESSAGE", PipelineMessageNotificationListener.class)
        .put("ASYNC-MESSAGE", AsyncMessageNotificationListener.class)
        .build();
  }

  public NotificationConfig(Class<L> interfaceClass, Class<N> eventClass) {
    this.interfaceClass = interfaceClass;
    this.eventClass = eventClass;
  }

  public NotificationConfig() {
    // used by the parser builder.
  }

  private String eventName;
  private Class<N> eventClass;
  private String interfaceName;
  private Class<L> interfaceClass;
  private boolean eventExplicitlyConfigured;
  private boolean interfaceExplicitlyConfigured;

  public void setInterfaceName(String interfaceName) {
    interfaceExplicitlyConfigured = true;
    this.interfaceName = interfaceName;
  }

  public String getInterfaceName() {
    return interfaceName;
  }

  public void setEventClass(Class<N> eventClass) {
    eventExplicitlyConfigured = true;
    this.eventClass = eventClass;
  }

  public Class<N> getEventClass() {
    return eventClass;
  }

  public void setEventName(String eventName) {
    eventExplicitlyConfigured = true;
    this.eventName = eventName;
  }

  public String getEventName() {
    return eventName;
  }

  public void setInterfaceClass(Class<L> interfaceClass) {
    interfaceExplicitlyConfigured = true;
    this.interfaceClass = interfaceClass;
  }

  public Class<L> getInterfaceClass() {
    return interfaceClass;
  }

  public boolean isEventExplicitlyConfigured() {
    return eventExplicitlyConfigured;
  }

  public boolean isInterfaceExplicitlyConfigured() {
    return interfaceExplicitlyConfigured;
  }

  public static class EnabledNotificationConfig<N extends Notification, L extends NotificationListener<N>>
      extends NotificationConfig<N, L> {

    public EnabledNotificationConfig(Class<L> interfaceClass, Class<N> eventClass) {
      super(interfaceClass, eventClass);
    }

    public EnabledNotificationConfig() {
      super();
    }
  }

  public static class DisabledNotificationConfig<N extends Notification, L extends NotificationListener<N>>
      extends NotificationConfig<N, L> {

    public DisabledNotificationConfig() {
      super();
    }
  }

}
