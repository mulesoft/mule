package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;

import static java.util.Collections.emptyMap;

import static org.junit.Assert.assertSame;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.security.UnauthorisedException;
import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DynamicServerNotificationManagerConfiguratorTestCase extends AbstractMuleContextTestCase {

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new SpringXmlConfigurationBuilder(new String[]{"./dynamic-server-notification-manager-test.xml"}, emptyMap());
  }

  @Test
  public void testRegistryHasNoGenericServerNotificationManagerIfDynamicConfigIsPresent() {
    assertSame(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_NOTIFICATION_MANAGER), null);
  }

  protected static interface TestInterface extends NotificationListener<TestEvent> {
    // empty
  }

  protected static interface TestInterface2 extends NotificationListener<Notification> {
    // empty
  }

  public static class TestListener implements TestInterface {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(TestEvent notification) {
      called = true;
    }

  }

  public static class TestListener2 implements TestInterface2 {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(Notification notification) {
      called = true;
    }

  }

  public static class TestSecurityListener implements SecurityNotificationListener<SecurityNotification> {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(SecurityNotification notification) {
      called = true;
    }

  }

  protected static class TestEvent extends AbstractServerNotification {

    public TestEvent() {
      super(new Object(), 0);
    }

    @Override
    public String getEventName() {
      return "TestEvent";
    }
  }

  protected static class TestSecurityEvent extends SecurityNotification {

    @Override
    public boolean isSynchronous() {
      return true;
    }

    public TestSecurityEvent(MuleContext muleContext) throws Exception {
      super(new UnauthorisedException(createStaticMessage("dummy")), 0);
    }

    @Override
    public String getEventName() {
      return "TestSecurityEvent";
    }
  }
}
