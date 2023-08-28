/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.privileged.util.TemplateParser.createMuleStyleParser;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findOperation;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.module.extension.internal.policy.NoOpPolicyManager;
import org.mule.runtime.module.extension.internal.runtime.client.operation.EventedOperationsParameterDecorator;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;

// TODO: Remove this class and use {@link OperationClient} once the performance issues are fixed.
/**
 * Provides help for executing extension operations.
 * <p>
 * The {@link OperationMessageProcessor}s created by this helper are cached in the cache given in the constructor.
 *
 * @deprecated Use {@link OperationClient}.
 */
@Deprecated
public class ExtensionsClientProcessorsHelper {

  private static final PolicyManager DEFAULT_POLICY_MANAGER = new NoOpPolicyManager();
  private static final String INTERNAL_VARIABLE_PREFIX = "INTERNAL_VARIABLE_";

  private final TemplateParser parser = createMuleStyleParser();

  private final ExtensionManager extensionManager;
  private final Registry registry;
  private final MuleContext muleContext;
  private final PolicyManager policyManager;
  private final ReflectionCache reflectionCache;
  private final Cache<String, OperationMessageProcessor> operationMessageProcessorCache;

  /**
   * Creates a new instance
   */
  public ExtensionsClientProcessorsHelper(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                          PolicyManager policyManager, ReflectionCache reflectionCache,
                                          Cache<String, OperationMessageProcessor> operationMessageProcessorCache) {
    this.extensionManager = extensionManager;
    this.registry = registry;
    this.muleContext = muleContext;
    this.policyManager = policyManager != null ? policyManager : DEFAULT_POLICY_MANAGER;
    this.reflectionCache = reflectionCache;
    this.operationMessageProcessorCache = operationMessageProcessorCache;
  }

  /**
   * @param extensionName the name of the extension to run the operation.
   * @param operationName the name of the operation to run.
   * @param parameters    the operation parameters used to run the operation
   * @return the appropriate {@link OperationMessageProcessor} to be used for executing the operation.
   */
  public OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                OperationParameters parameters) {
    String key = buildKey(extensionName, operationName, parameters);
    return operationMessageProcessorCache.get(key, (cacheKey) -> {
      Map<String, ValueResolver<?>> params = parameters.get().entrySet().stream()
          .collect(toMap(Map.Entry::getKey, e -> {
            ExpressionValueResolver<?> resolver =
                new ExpressionValueResolver<>(DEFAULT_EXPRESSION_PREFIX + "vars.'" + INTERNAL_VARIABLE_PREFIX + e.getKey() + "'"
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
   * @param parameters the {@link OperationParameters} used to execute the operation.
   * @return the appropriate event to be used to run de desired operation with the {@link OperationParameters} given.
   */
  public CoreEvent getEvent(OperationParameters parameters) {
    if (parameters instanceof EventedOperationsParameterDecorator) {
      Event contextEvent = ((EventedOperationsParameterDecorator) parameters).getContextEvent();
      if (contextEvent instanceof CoreEvent) {
        return buildChildEvent((CoreEvent) contextEvent, parameters);
      }
    }
    return buildChildEvent(getNullEvent(), parameters);
  }

  private OperationMessageProcessor createProcessor(String extensionName, String operationName, Optional<String> configName,
                                                    Map<String, ValueResolver<?>> parameters) {
    ExtensionModel extension = findExtension(extensionName);
    OperationModel operation = findOperation(extension, operationName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Operation [" + operationName + "] Found")));
    ConfigurationProvider config = configName.map(this::findConfiguration).orElse(null);
    try {
      OperationMessageProcessor processor =
          new OperationMessageProcessorBuilder(extension, operation, emptyList(), policyManager, muleContext, registry)
              .setConfigurationProvider(config)
              .setParameters(parameters)
              .setTerminationTimeout(-1)
              .build();

      initialiseIfNeeded(processor, muleContext);
      processor.start();
      return processor;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create Operation Message Processor"), e);
    }
  }

  private ConfigurationProvider findConfiguration(String configName) {
    return extensionManager.getConfigurationProvider(configName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configName + "] found")));
  }

  private ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }

  private CoreEvent buildChildEvent(CoreEvent event, OperationParameters parameters) {
    CoreEvent.Builder childEventBuilder = CoreEvent.builder(event);
    Map<String, ValueResolver<?>> operationParameters = resolveParameters(parameters.get(), event);
    for (String key : operationParameters.keySet()) {
      ValueResolver<?> valueResolver = operationParameters.get(key);
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
    keyBuilder.append(extension).append(separator)
        .append(operation).append(separator)
        .append(parameters.getConfigName().orElse(""));
    List<String> keyList = parameters.get().keySet()
        .stream()
        .sorted(naturalOrder())
        .collect(toList());
    for (String key : keyList) {
      keyBuilder.append(separator).append(key);
    }
    return keyBuilder.toString();
  }

  protected Map<String, ValueResolver<?>> resolveParameters(Map<String, Object> parameters, CoreEvent event) {
    LinkedHashMap<String, ValueResolver<?>> values = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      ValueResolver<?> valueResolver;
      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType(), reflectionCache);
        resolveParameters(complex.getParameters(), event).forEach(builder::addPropertyResolver);
        try {
          valueResolver = new StaticValueResolver<>(builder.build(ValueResolvingContext.builder(event).build()));
        } catch (MuleException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Could not construct parameter [%s]", name)), e);
        }
      } else {
        if (value instanceof String && parser.isContainsTemplate((String) value)) {
          valueResolver = new ExpressionValueResolver<>((String) value);
        } else {
          valueResolver = new StaticValueResolver<>(value);
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
