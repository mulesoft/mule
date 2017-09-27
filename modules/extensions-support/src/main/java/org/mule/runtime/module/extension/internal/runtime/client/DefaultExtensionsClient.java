/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
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
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  @Inject
  private PolicyManager policyManager;

  @Inject
  private ExtensionManager extensionManager;

  private final Map<Pair<String, String>, OperationModel> operations = new LinkedHashMap<>();
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension,
                                                             String operation,
                                                             OperationParameters parameters) {
    OperationMessageProcessor processor = createProcessor(extension, operation, parameters);
    Mono<Result<T, A>> resultMono = from(processor.apply(just(getInitialiserEvent(muleContext))))
        .map(event -> Result.<T, A>builder(event.getMessage()).build())
        .onErrorMap(Exceptions::unwrap)
        .doAfterTerminate(() -> disposeProcessor(processor));
    return resultMono.toFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {
    OperationMessageProcessor processor = createProcessor(extension, operation, params);
    try {
      CoreEvent process = processor.process(getInitialiserEvent(muleContext));
      return Result.<T, A>builder(process.getMessage()).build();
    } finally {
      disposeProcessor(processor);
    }
  }

  /**
   * Creates a new {@link OperationMessageProcessor} for the required operation and parses all the parameters passed by the client
   * user.
   */
  private OperationMessageProcessor createProcessor(String extensionName, String operationName, OperationParameters parameters) {
    ExtensionModel extension = findExtension(extensionName);
    OperationModel operation = findOperation(extension, operationName);
    ConfigurationProvider config = parameters.getConfigName().map(this::findConfiguration).orElse(null);
    Map<String, ValueResolver> resolvedParams = resolveParameters(parameters.get(), getInitialiserEvent(muleContext));
    try {
      OperationMessageProcessor processor =
          new OperationMessageProcessorBuilder(extension, operation, policyManager, muleContext, registry)
              .setConfigurationProvider(config)
              .setParameters(resolvedParams)
              .build();

      processor.initialise();
      processor.start();
      return processor;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create Operation Message Processor"), e);
    }
  }

  private Map<String, ValueResolver> resolveParameters(Map<String, Object> parameters, CoreEvent event) {
    LinkedHashMap<String, ValueResolver> values = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType());
        resolveParameters(complex.getParameters(), event).forEach((propertyName, valueResolver) -> {
          try {
            LifecycleUtils.initialiseIfNeeded(valueResolver, true, muleContext);
            builder.addPropertyResolver(propertyName, valueResolver);
          } catch (InitialisationException e) {
            throw new MuleRuntimeException(e);
          }
        });
        try {
          values.put(name, new StaticValueResolver<>(builder.build(from(event))));
        } catch (MuleException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Could not construct parameter [%s]", name)), e);
        }
      } else {
        if (value instanceof String && parser.isContainsTemplate((String) value)) {
          values.put(name, new ExpressionValueResolver((String) value));
        } else {
          values.put(name, new StaticValueResolver<>(value));
        }
      }
    });
    return values;
  }

  private OperationModel findOperation(ExtensionModel extensionModel, String operationName) {
    return operations.computeIfAbsent(new Pair<>(extensionModel.getName(), operationName), ope -> {
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
    });
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
}
