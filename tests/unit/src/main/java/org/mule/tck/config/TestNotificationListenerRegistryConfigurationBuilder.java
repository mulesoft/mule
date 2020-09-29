/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.config;

import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

public class TestNotificationListenerRegistryConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws RegistrationException {
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(NotificationListenerRegistry.REGISTRY_KEY,
                                                                         new DefaultNotificationListenerRegistry());
  }
}
