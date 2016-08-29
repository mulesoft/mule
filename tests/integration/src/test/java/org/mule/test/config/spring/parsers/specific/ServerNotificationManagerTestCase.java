/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.specific;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;

import org.junit.Test;

public class ServerNotificationManagerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/specific/server-notification-manager-test.xml";
  }

  @Test
  public void testDynamicAttribute() {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertTrue(manager.isNotificationDynamic());
  }

  @Test
  public void testRoutingConfiguration() {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertTrue(manager.getInterfaceToTypes().size() > 2);
    Object ifaces = manager.getInterfaceToTypes().get(TestInterface.class);
    assertNotNull(ifaces);
    assertTrue(ifaces instanceof Collection);
    assertTrue(((Collection) ifaces).contains(TestEvent.class));
    ifaces = manager.getInterfaceToTypes().get(TestInterface2.class);
    assertNotNull(ifaces);
    assertTrue(ifaces instanceof Collection);
    assertTrue(((Collection) ifaces).contains(SecurityNotification.class));
  }

  @Test
  public void testSimpleNotification() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    Collection listeners = manager.getListeners();
    // Now all transformers are registered as listeners in order to get a context disposing notification
    assertTrue(listeners.size() > 5);
    TestListener listener = (TestListener) muleContext.getRegistry().lookupObject("listener");
    assertNotNull(listener);
    assertFalse(listener.isCalled());
    manager.fireNotification(new TestEvent());
    Thread.sleep(1000); // asynch events
    assertTrue(listener.isCalled());
  }

  @Test
  public void testExplicitlyConiguredNotificationListenerRegistration() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertTrue(manager.getListeners()
        .contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry().lookupObject("listener"),
                                               null)));
    assertTrue(manager.getListeners()
        .contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry().lookupObject("listener2"),
                                               null)));
    assertTrue(manager.getListeners().contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
        .lookupObject("securityListener"), null)));
    assertTrue(manager.getListeners()
        .contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry().lookupObject("listener3"),
                                               "*")));
  }

  @Test
  public void testAdhocNotificationListenerRegistrations() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();

    // Not registered as ad-hoc listener with null subscription as this is defined
    // explicitly.
    assertFalse(manager.getListeners()
        .contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry().lookupObject("listener3"),
                                               null)));

    // Registered as configured
    assertTrue(manager.getListeners()
        .contains(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry().lookupObject("listener4"),
                                               null)));
  }

  @Test
  public void testDisabledNotification() throws Exception {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    Collection listeners = manager.getListeners();
    // Now all transformers are registered as listeners in order to get a context disposing notification
    assertTrue(listeners.size() > 5);
    TestListener2 listener2 = (TestListener2) muleContext.getRegistry().lookupObject("listener2");
    assertNotNull(listener2);
    assertFalse(listener2.isCalled());
    TestSecurityListener adminListener = (TestSecurityListener) muleContext.getRegistry().lookupObject("securityListener");
    assertNotNull(adminListener);
    assertFalse(adminListener.isCalled());
    manager.fireNotification(new TestSecurityEvent(muleContext));
    new PollingProber(2000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return listener2.isCalled();
      }

      @Override
      public String describeFailure() {
        return "listener2 should be notified";
      }
    });
    assertFalse(adminListener.isCalled());
  }

  protected static interface TestInterface extends ServerNotificationListener {
    // empty
  }

  protected static interface TestInterface2 extends ServerNotificationListener {
    // empty
  }

  protected static class TestListener implements TestInterface {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(ServerNotification notification) {
      called = true;
    }

  }

  protected static class TestListener2 implements TestInterface2 {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(ServerNotification notification) {
      called = true;
    }

  }

  protected static class TestSecurityListener implements SecurityNotificationListener<SecurityNotification> {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(SecurityNotification notification) {
      called = true;
    }

  }

  protected static class TestEvent extends ServerNotification {

    public TestEvent() {
      super(new Object(), 0);
    }

  }

  protected static class TestSecurityEvent extends SecurityNotification {

    public TestSecurityEvent(MuleContext muleContext) throws Exception {
      super(new UnauthorisedException(CoreMessages.createStaticMessage("dummy"),
                                      MuleEvent.builder(DefaultMessageContext.create(getTestFlow(), TEST_CONNECTOR))
                                          .message(MuleMessage.builder().nullPayload().build()).exchangePattern(REQUEST_RESPONSE)
                                          .flow(getTestFlow()).session(getTestSession(null, muleContext)).build()),
            0);
    }

  }

}
