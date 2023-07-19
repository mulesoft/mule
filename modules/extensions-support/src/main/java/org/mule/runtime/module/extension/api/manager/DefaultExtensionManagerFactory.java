/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * Default implementation of {@link ExtensionManagerFactory} which creates instances of {@link DefaultExtensionManager} and sets
 * them into the owning {@link MuleContext}
 *
 * @since 4.0
 */
public final class DefaultExtensionManagerFactory implements ExtensionManagerFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManager create(MuleContext muleContext) {
    ExtensionManager extensionManager = new DefaultExtensionManager();
    muleContext.setExtensionManager(extensionManager);
    try {
      initialiseIfNeeded(extensionManager, muleContext);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not initialise extension manager"), e);
    }

    return extensionManager;
  }
}
