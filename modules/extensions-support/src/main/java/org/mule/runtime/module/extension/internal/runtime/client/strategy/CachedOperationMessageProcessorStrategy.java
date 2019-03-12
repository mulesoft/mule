/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSED;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CachedOperationMessageProcessorStrategy extends OperationMessageProcessorStrategy {

  private static Map<String, Cache<String, OperationMessageProcessor>> operationMessageProcessorsCaches = new HashMap<>();

  private static String CONTEXT_DISPOSED_NOTIFICATION = String.valueOf(CONTEXT_DISPOSED);
  private static int MAX_CACHE_SIZE = 100;
  private static int CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES = 10;
  private static String INTERNAL_VARIABLE_PREFIX = "INTERNAL_VARIABLE_";

  public CachedOperationMessageProcessorStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                                 PolicyManager policyManager, ReflectionCache reflectionCache, CoreEvent event) {
    super(extensionManager, registry, muleContext, policyManager, reflectionCache, event);
  }

  @Override
  public OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                OperationParameters parameters) {
    String key = buildKey(extensionName, operationName, parameters);
    String muleContextId = muleContext.getId();
    if (!operationMessageProcessorsCaches.containsKey(muleContextId)) {
      operationMessageProcessorsCaches.put(muleContextId, createNewCache());
    }
    Cache<String, OperationMessageProcessor> operationMessageProcessorCache =
        operationMessageProcessorsCaches.get(muleContextId);
    return operationMessageProcessorCache.get(key, (cacheKey) -> {
      Map<String, ValueResolver> params = parameters.get().entrySet().stream()
          .collect(toMap(e -> e.getKey(), e -> {
            ExpressionValueResolver resolver =
                new ExpressionValueResolver(DEFAULT_EXPRESSION_PREFIX + "vars." + INTERNAL_VARIABLE_PREFIX + e.getKey()
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

  @Override
  public CoreEvent getEvent(OperationParameters parameters) {
    return event == null ? buildChildEvent(getBaseEvent(), parameters) : buildChildEvent(event, parameters);
  }

  @Override
  public void disposeProcessor(OperationMessageProcessor operationMessageProcessor) {
    // The processors does not need to be disposed here since we may reuse it later
  }


  private Cache<String, OperationMessageProcessor> createNewCache() {
    listenToMuleContextDisposalAndInvalidateCache();
    return Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .expireAfterAccess(CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES, TimeUnit.MINUTES)
        .<String, OperationMessageProcessor>removalListener((key, operationMessageProcessor,
                                                             removalCause) -> doDisposeProcessor(operationMessageProcessor))
        .build();
  }

  private void listenToMuleContextDisposalAndInvalidateCache() {
    muleContext.getNotificationManager().addListener(new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public void onNotification(MuleContextNotification notification) {
        if (notification.getMuleContext().getId().equals(muleContext.getId())
            && notification.getAction().getIdentifier().equals(CONTEXT_DISPOSED_NOTIFICATION)) {
          Cache<String, OperationMessageProcessor> cacheToInvalidate =
              operationMessageProcessorsCaches.remove(muleContext.getId());
          cacheToInvalidate.invalidateAll();
        }
      }

    });
  }

  private CoreEvent buildChildEvent(CoreEvent event, OperationParameters parameters) {
    CoreEvent.Builder childEventBuilder = CoreEvent.builder(event);
    Map<String, ValueResolver> operationParameters = resolveParameters(parameters.get(), event);
    for (String key : operationParameters.keySet()) {
      ValueResolver valueResolver = operationParameters.get(key);
      try {
        Object value = valueResolver.resolve(ValueResolvingContext.from(event));
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
    keyBuilder.append(extension).append(separator).append(operation).append(separator).append(parameters.getConfigName());
    List<String> keyList = parameters.get().keySet().stream().collect(Collectors.toList());
    Collections.sort(keyList, Comparator.naturalOrder());
    for (String key : keyList) {
      keyBuilder.append(separator).append(key);
    }
    return keyBuilder.toString();
  }

  private Map<String, ValueResolver> resolveParameters(Map<String, Object> parameters, CoreEvent event) {
    LinkedHashMap<String, ValueResolver> values = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      ValueResolver valueResolver;
      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType(), reflectionCache);
        resolveParameters(complex.getParameters(), event).forEach((propertyName, resolver) -> {
          builder.addPropertyResolver(propertyName, resolver);
        });
        try {
          valueResolver = new StaticValueResolver<>(builder.build(ValueResolvingContext.from(event)));
        } catch (MuleException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Could not construct parameter [%s]", name)), e);
        }
      } else {
        if (value instanceof String && parser.isContainsTemplate((String) value)) {
          valueResolver = new ExpressionValueResolver((String) value);
        } else {
          valueResolver = new StaticValueResolver(value);
        }
      }
      try {
        initialiseIfNeeded(valueResolver, true, muleContext);
      } catch (InitialisationException e) {
        throw new MuleRuntimeException(e);
      }
      values.put(name, valueResolver);
    });
    return values;
  }
}
