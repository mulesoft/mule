/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.manager;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

/**
 * Default implementation of {@link ExtensionManagerFactory} which creates instances of
 * {@link DefaultExtensionManager} and sets them into the owning {@link MuleContext}
 *
 * @since 4.0
 */
public class DefaultExtensionManagerFactory implements ExtensionManagerFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManager create(MuleContext muleContext) {
    ExtensionManager extensionManager = new DefaultExtensionManager();
    muleContext.setExtensionManager(extensionManager);
    try {
      initialiseIfNeeded(extensionManager, false, muleContext);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not initialise extension manager"), e);
    }

    return extensionManager;
  }
}
