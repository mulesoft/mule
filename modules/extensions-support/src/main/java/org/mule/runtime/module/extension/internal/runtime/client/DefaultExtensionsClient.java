/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import reactor.core.publisher.Mono;


/**
 * This is the default implementation for a {@link ExtensionsClient}, it uses the {@link ExtensionManager} in the
 * {@link MuleContext} to search for the extension that wants to execute the operation from.
 * <p>
 * The concrete execution of the operation is handled by an {@link OperationMessageProcessor} instance.
 * <p>
 * This implementation can only execute extensions that were built using the SDK, Smart Connectors operations can't be executed.
 *
 * @since 4.0
 */
public final class DefaultExtensionsClient implements ExtensionsClient {

  private final TemplateParser parser = TemplateParser.createMuleStyleParser();

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  @Inject
  private PolicyManager policyManager;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ReflectionCache reflectionCache;

  private final CoreEvent event;

  private static Map<String, OperationMessageProcessor> processorMap = new HashMap<>();

  /**
   * This constructor enables the {@link DefaultExtensionsClient} to be aware of the current execution {@link CoreEvent} and
   * enables to perform the dynamic operation execution with the same event that the SDK operation using the {@link ExtensionsClient}
   * receives.
   *
   * @param muleContext   the current context.
   * @param event         the current execution event.
   * @param registry      the application registry.
   * @param policyManager the configured application policy manager.
   */
  public DefaultExtensionsClient(MuleContext muleContext, CoreEvent event, Registry registry, PolicyManager policyManager) {
    this.muleContext = muleContext;
    this.event = event;
    this.extensionManager = muleContext.getExtensionManager();
    this.registry = registry;
    this.policyManager = policyManager;
  }

  /**
   * Creating a client from this constructor will enable the execution of operations with an initializer event.
   */
  public DefaultExtensionsClient() {
    this.event = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation, OperationParameters parameters) {
    OperationMessageProcessor processor = getOrCreateOperationMessageProcessor(extension, operation, parameters);
    return just(buildChildEvent(event, parameters)).transform(processor)
        .map(event -> Result.<T, A>builder(event.getMessage()).build())
        .onErrorMap(Exceptions::unwrap)
        .doAfterTerminate(() -> disposeProcessor(processor))
        .toFuture();
  }

  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {
    OperationMessageProcessor processor = getOrCreateOperationMessageProcessor(extension, operation, params);
    try {
      CoreEvent process = processor.process(buildChildEvent(getEvent(), params));
      return Result.<T, A>builder(process.getMessage()).build();
    } finally {
      disposeProcessor(processor);
    }
  }

  private OperationMessageProcessor getOrCreateOperationMessageProcessor(String extension, String operation,
                                                                         OperationParameters parameters) {
    String key = buildKey(extension, operation, parameters);
    if (!processorMap.containsKey(key)) {
      Map<String, ValueResolver> params = parameters.get().entrySet().stream()
          .collect(toMap(e -> e.getKey(), e -> {
            ExpressionValueResolver resolver =
                new ExpressionValueResolver(DEFAULT_EXPRESSION_PREFIX + "vars." + e.getKey() + DEFAULT_EXPRESSION_POSTFIX);
            try {
              initialiseIfNeeded(resolver, true, muleContext);
            } catch (InitialisationException ex) {
              throw new MuleRuntimeException(ex);
            }
            return resolver;
          }));
      processorMap.put(key, createProcessor(extension, operation, parameters.getConfigName(), params));
    }
    return processorMap.get(key);
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

  private Mono<CoreEvent> process(OperationMessageProcessor omp, OperationParameters parameters) {
    if (event != null) {
      return from(processWithChildContext(buildChildEvent(event, parameters), omp, Optional.empty()));
    }
    return from(omp.apply(just(buildChildEvent(getInitialiserEvent(muleContext), parameters))));
  }

  private CoreEvent buildChildEvent(CoreEvent event, OperationParameters parameters) {
    if(event == null){
      return getInitialiserEvent();
    }
    CoreEvent.Builder childEventBuilder = CoreEvent.builder(event);
    Map<String, ValueResolver> operationParameters = resolveParameters(parameters.get(), event);
    for (String key : operationParameters.keySet()) {
      ValueResolver valueResolver = operationParameters.get(key);
      try {
        Object value = valueResolver.resolve(ValueResolvingContext.from(event));
        childEventBuilder.addVariable(key, value, DataType.fromObject(value));
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return childEventBuilder.build();
  }

  /**
   * Creates a new {@link OperationMessageProcessor} for the required operation and parses all the parameters passed by the client
   * user.
   */
  private OperationMessageProcessor createProcessor(String extensionName, String operationName, Optional<String> configName,
                                                    Map<String, ValueResolver> parameters) {
    ExtensionModel extension = findExtension(extensionName);
    OperationModel operation = findOperation(extension, operationName);
    ConfigurationProvider config = configName.map(this::findConfiguration).orElse(null);
    Map<String, ValueResolver> resolvedParams = parameters;
    try {
      OperationMessageProcessor processor =
          new OperationMessageProcessorBuilder(extension, operation, policyManager, muleContext, registry)
              .setConfigurationProvider(config)
              .setParameters(resolvedParams)
              .build();

      initialiseIfNeeded(processor, muleContext);
      processor.start();
      return processor;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create Operation Message Processor"), e);
    }
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

  private OperationModel findOperation(ExtensionModel extensionModel, String operationName) {
    Reference<OperationModel> operation = new Reference<>();
    ExtensionWalker walker = new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operationModel) {
        if (operationName.equals(operationModel.getName())) {
          operation.set(operationModel);
          stop();
        }
      }
    };
    walker.walk(extensionModel);
    if (operation.get() == null) {
      throw new MuleRuntimeException(createStaticMessage("No Operation [" + operationName + "] Found"));
    }
    return operation.get();
  }

  private ConfigurationProvider findConfiguration(String configName) {
    return extensionManager.getConfigurationProvider(configName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configName + "] found")));
  }

  private ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }

  private void disposeProcessor(OperationMessageProcessor processor) {
    try {
      processor.stop();
      processor.dispose();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while disposing the executing operation"), e);
    }
  }

  private CoreEvent getEvent() {
    return event == null ? getInitialiserEvent() : event;
  }
}
