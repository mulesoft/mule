package org.mule.runtime.config.internal.context;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;

import static java.util.Collections.emptyMap;

import static org.junit.Assert.assertSame;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DynamicServerNotificationManagerConfiguratorTestCase extends AbstractMuleContextTestCase {

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new SpringXmlConfigurationBuilder(new String[] {"./dynamic-server-notification-manager-test.xml"}, emptyMap());
  }

  @Test
  public void testRegistryHasNoGenericServerNotificationManagerIfDynamicConfigIsPresent() {
    assertSame(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_NOTIFICATION_MANAGER), null);
  }
}
