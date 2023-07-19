/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_REGISTRY_BOOTSTRAP_OPTIONAL_ENTRIES_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertSame;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

public class DynamicServerNotificationManagerConfiguratorTestCase extends AbstractMuleContextTestCase {

  // TODO W-10736276 Remove this
  @Rule
  public SystemProperty systemProperty = new SystemProperty(DISABLE_REGISTRY_BOOTSTRAP_OPTIONAL_ENTRIES_PROPERTY, "false");

  @Override
  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new SpringXmlConfigurationBuilder(new String[] {"./dynamic-server-notification-manager-test.xml"}, emptyMap());
  }

  @Test
  public void testRegistryHasNoGenericServerNotificationManagerIfDynamicConfigIsPresent() {
    assertSame(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_NOTIFICATION_MANAGER), null);
  }
}
