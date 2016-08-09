/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

/**
 * Default implementation of {@link ExtensionManagerAdapterFactory} which creates instances of
 * {@link DefaultExtensionManagerAdapterFactory} and sets them into the owning {@link MuleContext}
 *
 * @since 4.0
 */
public class DefaultExtensionManagerAdapterFactory implements ExtensionManagerAdapterFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) {
    ExtensionManagerAdapter extensionManager = new DefaultExtensionManager();
    ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
    try {
      initialiseIfNeeded(extensionManager, muleContext);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not initialise extension manager"), e);
    }

    return extensionManager;
  }
}
