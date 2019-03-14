/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.lang.Boolean.parseBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXTENSIONS_CLIENT_CACHE_DISABLED;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

/**
 * Class that provides a {@link OperationMessageProcessorStrategy} according to the value of the system property
 * MULE_EXTENSIONS_CLIENT_CACHE_DISABLED defined in {@link org.mule.runtime.api.util.MuleSystemProperties}
 *
 * @since 4.1.6
 */
public class OperationMessageProcessorStrategyFactory {

  /**
   * This method return the suitable {@link OperationMessageProcessorStrategy}
   */
  public static OperationMessageProcessorStrategy create(ExtensionManager extensionManager, Registry registry,
                                                         MuleContext muleContext, PolicyManager policyManager,
                                                         ReflectionCache reflectionCache, CoreEvent event) {
    return parseBoolean(System.getProperty(MULE_EXTENSIONS_CLIENT_CACHE_DISABLED))
        ? new NonCachedOperationMessageProcessorStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                         event)
        : new CachedOperationMessageProcessorStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                      event);
  }

}
