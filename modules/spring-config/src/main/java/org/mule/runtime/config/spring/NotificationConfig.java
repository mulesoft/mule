/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ComponentMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ComponentMessageNotification;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.ErrorHandlerNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.RegistryNotification;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.api.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Notification helper object for creating notification manager configuration.
 *
 * For now we create specific types {@code Notification.EnabledNotification} and {@code Notification.DisabledNotification} for
 * elements that have children of the same type but that must be injected in different collections of the parent object. Ideally
 * this should not be needed or we don't have to support such use case anymore but for previous elements coming from previous mule
 * version we have to keep the XML syntax as it is.
 *
 * @since 4.0
 */
public class NotificationConfig extends AbstractAnnotatedObject {

  private static final String ENDPOINT_MESSAGE_NOTIFICATION_CLASS =
      "com.mulesoft.mule.compatibility.core.context.notification.EndpointMessageNotification";
  private static final String ENDPOINT_MESSAGE_NOTIFICATION_LISTENER_CLASS =
      "com.mulesoft.mule.compatibility.core.api.context.notification.EndpointMessageNotificationListener";
  private static final Map<String, String> EVENT_MAP;
  private static final Map<String, String> INTERFACE_MAP;

  static {
    EVENT_MAP = new HashMap();
    EVENT_MAP.put("CONTEXT", MuleContextNotification.class.getName());
    EVENT_MAP.put("SECURITY", SecurityNotification.class.getName());
    EVENT_MAP.put("ENDPOINT-MESSAGE", ENDPOINT_MESSAGE_NOTIFICATION_CLASS);
    EVENT_MAP.put("CONNECTOR-MESSAGE", ConnectorMessageNotification.class.getName());
    EVENT_MAP.put("COMPONENT-MESSAGE", ComponentMessageNotification.class.getName());
    EVENT_MAP.put("MANAGEMENT", ManagementNotification.class.getName());
    EVENT_MAP.put("MESSAGE-PROCESSOR", MessageProcessorNotification.class.getName());
    EVENT_MAP.put("EXCEPTION-STRATEGY", ErrorHandlerNotification.class.getName());
    EVENT_MAP.put("CONNECTION", ConnectionNotification.class.getName());
    EVENT_MAP.put("REGISTRY", RegistryNotification.class.getName());
    EVENT_MAP.put("CUSTOM", CustomNotification.class.getName());
    EVENT_MAP.put("EXCEPTION", ExceptionNotification.class.getName());
    EVENT_MAP.put("TRANSACTION", TransactionNotification.class.getName());
    EVENT_MAP.put("ROUTING", RoutingNotification.class.getName());
    EVENT_MAP.put("PIPELINE-MESSAGE", PipelineMessageNotification.class.getName());
    EVENT_MAP.put("ASYNC-MESSAGE", AsyncMessageNotification.class.getName());
    // EVENT_MAP.put("MESSAGE", ConnectorMessageNotification.class.getName());

    INTERFACE_MAP = new HashMap();
    INTERFACE_MAP.put("CONTEXT", MuleContextNotificationListener.class.getName());
    INTERFACE_MAP.put("SECURITY", SecurityNotificationListener.class.getName());
    INTERFACE_MAP.put("MANAGEMENT", ManagementNotificationListener.class.getName());
    INTERFACE_MAP.put("MESSAGE-PROCESSOR", MessageProcessorNotificationListener.class.getName());
    INTERFACE_MAP.put("EXCEPTION-STRATEGY", ErrorHandlerNotificationListener.class.getName());
    INTERFACE_MAP.put("CONNECTION", ConnectionNotificationListener.class.getName());
    INTERFACE_MAP.put("REGISTRY", RegistryNotificationListener.class.getName());
    INTERFACE_MAP.put("CUSTOM", CustomNotificationListener.class.getName());
    INTERFACE_MAP.put("ENDPOINT-MESSAGE", ENDPOINT_MESSAGE_NOTIFICATION_LISTENER_CLASS);
    INTERFACE_MAP.put("CONNECTOR-MESSAGE", ConnectorMessageNotificationListener.class.getName());
    INTERFACE_MAP.put("COMPONENT-MESSAGE", ComponentMessageNotificationListener.class.getName());
    INTERFACE_MAP.put("EXCEPTION", ExceptionNotificationListener.class.getName());
    INTERFACE_MAP.put("TRANSACTION", TransactionNotificationListener.class.getName());
    INTERFACE_MAP.put("ROUTING", RoutingNotificationListener.class.getName());
    INTERFACE_MAP.put("CLUSTER-NODE", ClusterNodeNotificationListener.class.getName());
    INTERFACE_MAP.put("PIPELINE-MESSAGE", PipelineMessageNotification.class.getName());
    INTERFACE_MAP.put("ASYNC-MESSAGE", AsyncMessageNotification.class.getName());

    // Deprecated
    EVENT_MAP.put("MESSAGE", ENDPOINT_MESSAGE_NOTIFICATION_CLASS);
  }

  public NotificationConfig(Class interfaceClass, Class eventClass) {
    this.interfaceClass = interfaceClass;
    this.eventClass = eventClass;
  }

  public NotificationConfig() {
    // used by the parser builder.
  }

  private String eventName;
  private Class eventClass;
  private String interfaseName;
  private Class interfaceClass;
  private boolean eventExplicitlyConfigured;
  private boolean interfaceExplicitlyConfigured;

  public void setInterfaseName(String interfaseName) {
    interfaceExplicitlyConfigured = true;
    this.interfaseName = interfaseName;
  }

  public void setEventClass(Class eventClass) {
    eventExplicitlyConfigured = true;
    this.eventClass = eventClass;
  }

  public void setEventName(String eventName) {
    eventExplicitlyConfigured = true;
    this.eventName = eventName;
  }

  public void setInterfaceClass(Class interfaceClass) {
    interfaceExplicitlyConfigured = true;
    this.interfaceClass = interfaceClass;
  }

  public Optional<Class> getEventClass() {
    if (eventClass != null) {
      return of(eventClass);
    }
    if (eventName != null) {
      return of(loadClass(EVENT_MAP.get(eventName)));
    }
    return ofNullable(loadClass(EVENT_MAP.get(interfaseName)));
  }

  private Class loadClass(String className) {
    if (className == null) {
      return null;
    }
    try {
      return ClassUtils.loadClass(className, Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public Optional<Class> getInterfaceClass() {
    if (interfaceClass != null) {
      return of(interfaceClass);
    }
    if (interfaseName != null) {
      return of(loadClass(INTERFACE_MAP.get(interfaseName)));
    }
    return ofNullable(loadClass(INTERFACE_MAP.get(eventName)));
  }

  public boolean isEventExplicitlyConfigured() {
    return eventExplicitlyConfigured;
  }

  public boolean isInterfaceExplicitlyConfigured() {
    return interfaceExplicitlyConfigured;
  }

  /**
   * Notifications that must be enabled
   */
  public static class EnabledNotificationConfig extends NotificationConfig {

    public EnabledNotificationConfig(Class interfaceClass, Class eventClass) {
      super(interfaceClass, eventClass);
    }

    public EnabledNotificationConfig() {}
  }

  /**
   * Notifications that must be disabled
   */
  public static class DisabledNotificationConfig extends NotificationConfig {

    public DisabledNotificationConfig(Class interfaceClass, Class eventClass) {
      super(interfaceClass, eventClass);
    }

    public DisabledNotificationConfig() {}
  }
}
