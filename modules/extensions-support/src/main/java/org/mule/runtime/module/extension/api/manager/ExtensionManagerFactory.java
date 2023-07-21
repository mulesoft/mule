/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.manager;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;

/**
 * Factory to create instances of {@link ExtensionManager}
 *
 * @since 4.0
 */
@NoImplement
public interface ExtensionManagerFactory {

  /**
   * Creates a new {@link ExtensionManager}
   *
   * @param muleContext the owning {@link MuleContext}
   * @return a non {@code null} {@link ExtensionManager}
   */
  ExtensionManager create(MuleContext muleContext);
}
