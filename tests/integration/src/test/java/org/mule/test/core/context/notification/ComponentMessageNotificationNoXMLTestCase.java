/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.ComponentMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.component.simple.EchoComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.ComponentMessageNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;

import org.junit.Test;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and post notification should be received by
 * listeners.
 */
public class ComponentMessageNotificationNoXMLTestCase extends AbstractMuleContextTestCase {

  protected Component component;
  protected ServerNotificationManager manager;
  protected ComponentListener componentListener;

  public ComponentMessageNotificationNoXMLTestCase() {
    setDisposeContextPerClass(true);
  }

  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    ServerNotificationManager notificationManager = new ServerNotificationManager();
    notificationManager.setNotificationDynamic(true);
    notificationManager.addInterfaceToType(ComponentMessageNotificationListener.class, ComponentMessageNotification.class);
    contextBuilder.setNotificationManager(notificationManager);
  }

  @Override
  protected void doSetUp() throws Exception {
    setDisposeContextPerClass(true);

    Flow flow = new Flow("testFlow", muleContext);
    componentListener = new ComponentListener();
    component = new DefaultJavaComponent(new SingletonObjectFactory(EchoComponent.class));
    flow.setMessageProcessors(Collections.<MessageProcessor>singletonList(component));
    muleContext.getRegistry().registerFlowConstruct(flow);

    if (!muleContext.isStarted()) {
      muleContext.start();
    }
  }

  @Test
  public void testComponentNotificationNotRegistered() throws Exception {
    assertFalse(componentListener.isNotified());

    component.process(MuleTestUtils.getTestEvent("test data", muleContext));

    assertFalse(componentListener.isNotified());
    assertFalse(componentListener.isBefore());
    assertFalse(componentListener.isAfter());
  }

  @Test
  public void testComponentNotification() throws Exception {
    // Need to configure NotificationManager as "dynamic" in order to do this.
    muleContext.registerListener(componentListener);

    assertFalse(componentListener.isNotified());

    component.process(MuleTestUtils.getTestEvent("test data", muleContext));

    // threaded processing, make sure the notifications have time to process
    Thread.sleep(100);

    assertTrue(componentListener.isNotified());
    assertTrue(componentListener.isBefore());
    assertTrue(componentListener.isAfter());
  }

  class ComponentListener implements ComponentMessageNotificationListener {

    private ServerNotification notification = null;

    private boolean before = false;

    private boolean after = false;

    public void onNotification(ServerNotification notification) {
      this.notification = notification;
      assertEquals(ComponentMessageNotification.class, notification.getClass());
      assertTrue(notification.getSource() instanceof MuleMessage);
      assertNotNull(((ComponentMessageNotification) notification).getServiceName());

      if (notification.getAction() == ComponentMessageNotification.COMPONENT_PRE_INVOKE) {
        before = true;
      } else if (notification.getAction() == ComponentMessageNotification.COMPONENT_POST_INVOKE) {
        after = true;
      }
    }

    public boolean isNotified() {
      return null != notification;
    }

    /**
     * @return the before
     */
    public boolean isBefore() {
      return before;
    }

    /**
     * @return the after
     */
    public boolean isAfter() {
      return after;
    }

  }

}
