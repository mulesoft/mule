/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.lang.Boolean.parseBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED;
import static org.mule.runtime.module.extension.internal.runtime.client.strategy.OperationMessageProcessorUtils.disposeProcessor;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javax.inject.Inject;

/**
 * Class that provides a {@link OperationMessageProcessorStrategy} according to the value of the system property
 * MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED defined in {@link org.mule.runtime.api.util.MuleSystemProperties}
 *
 * @since 4.1.6
 */
public class OperationMessageProcessorStrategyFactory implements Initialisable, Disposable {

  private static int MAX_CACHE_SIZE = 100;
  private static int CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES = 10;

  private boolean usesCachedStrategy;
  private Cache<String, OperationMessageProcessor> operationMessageProcessorCache;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private Registry registry;

  @Inject
  private MuleContext muleContext;

  @Inject
  private PolicyManager policyManager;

  @Inject
  private ReflectionCache reflectionCache;

  public OperationMessageProcessorStrategyFactory() {

  }

  /**
   * This method return the suitable {@link OperationMessageProcessorStrategy}
   */
  public OperationMessageProcessorStrategy create(CoreEvent event) {
    return usesCachedStrategy
        ? new CachedOperationMessageProcessorStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                      event, operationMessageProcessorCache)
        : new NonCachedOperationMessageProcessorStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                         event);
  }

  @Override
  public void initialise() throws InitialisationException {
    usesCachedStrategy = !parseBoolean(System.getProperty(MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED));
    if (usesCachedStrategy) {
      operationMessageProcessorCache = createCache();
    }
  }

  private Cache<String, OperationMessageProcessor> createCache() {
    return Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .expireAfterAccess(CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES, TimeUnit.MINUTES)
        .<String, OperationMessageProcessor>removalListener((key, operationMessageProcessor,
                                                             removalCause) -> disposeProcessor(operationMessageProcessor))
        .build();
  }

  @Override
  public void dispose() {
    if (operationMessageProcessorCache != null) {
      operationMessageProcessorCache.invalidateAll();
    }
  }

}
