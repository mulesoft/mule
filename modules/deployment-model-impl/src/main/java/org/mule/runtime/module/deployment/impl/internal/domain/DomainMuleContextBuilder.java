/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.api.notification.ClusterNodeNotificationListener;
import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.api.notification.ConnectionNotificationListener;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.ManagementNotification;
import org.mule.runtime.api.notification.ManagementNotificationListener;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

/**
 * Builder for domain MuleContext instance.
 */
public class DomainMuleContextBuilder extends DefaultMuleContextBuilder {

  private final String domainId;

  public DomainMuleContextBuilder(String domainId) {
    super(DOMAIN);
    this.domainId = domainId;
  }

  @Override
  protected MuleConfiguration getMuleConfiguration() {
    DefaultMuleConfiguration defaultMuleConfiguration = new DefaultMuleConfiguration(true);
    defaultMuleConfiguration.setDomainId(domainId);
    defaultMuleConfiguration.setId(domainId);
    return defaultMuleConfiguration;
  }

  @Override
  protected ServerNotificationManager createNotificationManager() {
    ServerNotificationManager manager = new ServerNotificationManager();
    manager.addInterfaceToType(MuleContextNotificationListener.class, MuleContextNotification.class);
    manager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);
    manager.addInterfaceToType(ManagementNotificationListener.class, ManagementNotification.class);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
    manager.addInterfaceToType(ConnectionNotificationListener.class, ConnectionNotification.class);
    manager.addInterfaceToType(ExceptionNotificationListener.class, ExceptionNotification.class);
    manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
    return manager;
  }
}
