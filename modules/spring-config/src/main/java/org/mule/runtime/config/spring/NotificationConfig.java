/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotification;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.api.context.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotification;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotification;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotification;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotification;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotification;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotification;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotification;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;

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
public abstract class NotificationConfig<N extends ServerNotification, L extends ServerNotificationListener<N>>
    extends AbstractAnnotatedObject {

  public static final Map<String, Class<? extends ServerNotification>> EVENT_MAP;
  public static final Map<String, Class<? extends ServerNotificationListener>> INTERFACE_MAP;

  static {
    EVENT_MAP = ImmutableMap.<String, Class<? extends ServerNotification>>builder()
        .put("CONTEXT", MuleContextNotification.class)
        .put("SECURITY", SecurityNotification.class)
        .put("CONNECTOR-MESSAGE", ConnectorMessageNotification.class)
        .put("MANAGEMENT", ManagementNotification.class)
        .put("MESSAGE-PROCESSOR", MessageProcessorNotification.class)
        .put("EXCEPTION-STRATEGY", ErrorHandlerNotification.class)
        .put("CONNECTION", ConnectionNotification.class)
        .put("REGISTRY", RegistryNotification.class)
        .put("CUSTOM", CustomNotification.class)
        .put("EXCEPTION", ExceptionNotification.class)
        .put("TRANSACTION", TransactionNotification.class)
        .put("ROUTING", RoutingNotification.class)
        .put("PIPELINE-MESSAGE", PipelineMessageNotification.class)
        .put("ASYNC-MESSAGE", AsyncMessageNotification.class)
        .build();

    INTERFACE_MAP = ImmutableMap.<String, Class<? extends ServerNotificationListener>>builder()
        .put("CONTEXT", MuleContextNotificationListener.class)
        .put("SECURITY", SecurityNotificationListener.class)
        .put("MANAGEMENT", ManagementNotificationListener.class)
        .put("MESSAGE-PROCESSOR", MessageProcessorNotificationListener.class)
        .put("EXCEPTION-STRATEGY", ErrorHandlerNotificationListener.class)
        .put("CONNECTION", ConnectionNotificationListener.class)
        .put("REGISTRY", RegistryNotificationListener.class)
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

  public static class EnabledNotificationConfig<N extends ServerNotification, L extends ServerNotificationListener<N>>
      extends NotificationConfig<N, L> {

    public EnabledNotificationConfig(Class<L> interfaceClass, Class<N> eventClass) {
      super(interfaceClass, eventClass);
    }

    public EnabledNotificationConfig() {
      super();
    }
  }

  public static class DisabledNotificationConfig<N extends ServerNotification, L extends ServerNotificationListener<N>>
      extends NotificationConfig<N, L> {

    public DisabledNotificationConfig() {
      super();
    }
  }

}
