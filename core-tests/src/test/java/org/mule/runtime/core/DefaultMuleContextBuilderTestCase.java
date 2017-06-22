/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.api.context.notification.ConnectionNotification;
import org.mule.runtime.core.api.context.notification.CustomNotification;
import org.mule.runtime.core.api.context.notification.ExceptionNotification;
import org.mule.runtime.core.api.context.notification.ManagementNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.context.notification.RegistryNotification;
import org.mule.runtime.core.api.context.notification.RoutingNotification;
import org.mule.runtime.core.api.context.notification.SecurityNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.context.notification.TransactionNotification;
import org.mule.runtime.core.internal.lifecycle.MuleContextLifecycleManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class DefaultMuleContextBuilderTestCase extends AbstractMuleTestCase {

  @Test
  public void testBuildMuleContextDefault() {
    // Build
    DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
    MuleContext muleContext = builder.buildMuleContext();

    // Assert
    assertThat(muleContext, notNullValue());
    assertThat(muleContext.getConfiguration(), instanceOf(DefaultMuleConfiguration.class));
    assertThat(muleContext.getLifecycleManager(), instanceOf(MuleContextLifecycleManager.class));
    assertThat(muleContext.getNotificationManager(), instanceOf(ServerNotificationManager.class));
  }

  @Test
  public void testBuildMuleContextCustom() {
    // Build
    MuleContext muleContext = build();

    // Assert
    assertThat(muleContext, notNullValue());
    assertThat(muleContext.getConfiguration(), instanceOf(MyMuleConfiguration.class));
    assertThat(muleContext.getLifecycleManager(), instanceOf(MyLifeCycleManager.class));
    assertThat(muleContext.getNotificationManager(), instanceOf(MyServerNotificationManager.class));
  }

  /**
   * <p>
   * After the mule context is built it has to contain the proper notification interfaces in the notification manager
   * </p>
   */
  @Test
  public void notificationManagerContainsTheCorrectInterfaces() {
    DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
    builder.setMuleConfiguration(new MyMuleConfiguration());
    builder.setLifecycleManager(new MyLifeCycleManager());
    MuleContext muleContext = builder.buildMuleContext();

    Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaces =
        muleContext.getNotificationManager().getInterfaceToTypes();

    assertEquals(MuleContextNotification.class, interfaces.get(MuleContextNotificationListener.class).toArray()[0]);
    assertEquals(RoutingNotification.class, interfaces.get(RoutingNotificationListener.class).toArray()[0]);
    assertEquals(SecurityNotification.class, interfaces.get(SecurityNotificationListener.class).toArray()[0]);
    assertEquals(ManagementNotification.class, interfaces.get(ManagementNotificationListener.class).toArray()[0]);
    assertEquals(CustomNotification.class, interfaces.get(CustomNotificationListener.class).toArray()[0]);
    assertEquals(ConnectionNotification.class, interfaces.get(ConnectionNotificationListener.class).toArray()[0]);
    assertEquals(RegistryNotification.class, interfaces.get(RegistryNotificationListener.class).toArray()[0]);
    assertEquals(ExceptionNotification.class, interfaces.get(ExceptionNotificationListener.class).toArray()[0]);
    assertEquals(TransactionNotification.class, interfaces.get(TransactionNotificationListener.class).toArray()[0]);
    assertEquals(PipelineMessageNotification.class, interfaces.get(PipelineMessageNotificationListener.class).toArray()[0]);
    assertEquals(AsyncMessageNotification.class, interfaces.get(AsyncMessageNotificationListener.class).toArray()[0]);
    assertEquals(ClusterNodeNotification.class, interfaces.get(ClusterNodeNotificationListener.class).toArray()[0]);


  }

  private MuleContext build() {
    DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
    builder.setMuleConfiguration(new MyMuleConfiguration());
    builder.setLifecycleManager(new MyLifeCycleManager());
    builder.setNotificationManager(new MyServerNotificationManager());
    return builder.buildMuleContext();
  }

  /**
   * Override, we don't want a {@link MuleContext} created for this test case.
   */
  protected MuleContext createMuleContext() throws Exception {
    return null;
  }

  static class MyLifeCycleManager extends MuleContextLifecycleManager {
    // subclass just for testing
  }

  static class MyMuleConfiguration extends DefaultMuleConfiguration {
    // subclass just for testing
  }

  static class MyServerNotificationManager extends ServerNotificationManager {
    // subclass just for testing
  }
}
