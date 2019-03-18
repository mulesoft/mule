/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * {@link ExtensionsClientProcessorsStrategy} that reuses instances of {@link OperationMessageProcessor} for executions when
 * possible.
 *
 * @since 4.1.6
 */
public class CachedExtensionsClientProcessorsStrategy extends AbstractExtensionsClientProcessorsStrategy {

  private static final String INTERNAL_VARIABLE_PREFIX = "INTERNAL_VARIABLE_";

  private Cache<String, OperationMessageProcessor> operationMessageProcessorCache;

  /**
   * Creates a new instance
   */
  public CachedExtensionsClientProcessorsStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                                  PolicyManager policyManager, ReflectionCache reflectionCache, CoreEvent event,
                                                  Cache<String, OperationMessageProcessor> operationMessageProcessorCache) {
    super(extensionManager, registry, muleContext, policyManager, reflectionCache, event);
    this.operationMessageProcessorCache = operationMessageProcessorCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                OperationParameters parameters) {
    String key = buildKey(extensionName, operationName, parameters);
    return operationMessageProcessorCache.get(key, (cacheKey) -> {
      Map<String, ValueResolver> params = parameters.get().entrySet().stream()
          .collect(toMap(e -> e.getKey(), e -> {
            ExpressionValueResolver resolver =
                new ExpressionValueResolver(DEFAULT_EXPRESSION_PREFIX + "vars.'" + INTERNAL_VARIABLE_PREFIX + e.getKey() + "'"
                    + DEFAULT_EXPRESSION_POSTFIX);
            try {
              initialiseIfNeeded(resolver, true, muleContext);
            } catch (InitialisationException ex) {
              throw new MuleRuntimeException(ex);
            }
            return resolver;
          }));
      return createProcessor(extensionName, operationName, parameters.getConfigName(), params);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CoreEvent getEvent(OperationParameters parameters) {
    return event == null ? buildChildEvent(getBaseEvent(), parameters) : buildChildEvent(event, parameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disposeProcessor(OperationMessageProcessor operationMessageProcessor) {
    // The processors does not need to be disposed here since we may reuse it later
  }

  private CoreEvent buildChildEvent(CoreEvent event, OperationParameters parameters) {
    CoreEvent.Builder childEventBuilder = CoreEvent.builder(event);
    Map<String, ValueResolver> operationParameters = resolveParameters(parameters.get(), event);
    for (String key : operationParameters.keySet()) {
      ValueResolver valueResolver = operationParameters.get(key);
      try {
        Object value = valueResolver.resolve(ValueResolvingContext.builder(event).build());
        childEventBuilder.addVariable(INTERNAL_VARIABLE_PREFIX + key, value, DataType.fromObject(value));
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return childEventBuilder.build();
  }

  private String buildKey(String extension, String operation, OperationParameters parameters) {
    char separator = '&';
    StringBuilder keyBuilder = new StringBuilder(256);
    keyBuilder.append(extension).append(separator).append(operation).append(separator)
        .append(parameters.getConfigName().orElse(""));
    List<String> keyList = parameters.get().keySet().stream().collect(Collectors.toList());
    Collections.sort(keyList, Comparator.naturalOrder());
    for (String key : keyList) {
      keyBuilder.append(separator).append(key);
    }
    return keyBuilder.toString();
  }

}
