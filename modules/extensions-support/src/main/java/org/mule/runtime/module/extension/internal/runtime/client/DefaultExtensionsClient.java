/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorFactory.getOperationMessageProcessor;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;


/**
 * This is the default implementation for a {@link ExtensionsClient}, it uses the {@link ExtensionManager}
 * in the {@link MuleContext} to search for the extension that wants to execute the operation from.
 * <p>
 * The concrete execution of the operation is handled by an {@link OperationMessageProcessor} instance.
 * <p>
 * This implementation can only execute extensions that were built using the SDK, Smart Connectors operations can't be executed.
 *
 * @since 4.0
 */
public final class DefaultExtensionsClient implements ExtensionsClient, Initialisable {

  @Inject
  private MuleContext muleContext;

  @Inject
  private PolicyManager policyManager;

  private final Map<Pair<String, String>, OperationModel> operations = new LinkedHashMap<>();
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();

  private ExtensionManager extensionManager;

  /**
   * {@inheritDoc}
   * <p>
   * Set's up the {@link PolicyManager} and the {@link ExtensionManager} used to execute the operations.
   */
  @Override
  public void initialise() throws InitialisationException {
    this.extensionManager = muleContext.getExtensionManager();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A extends Attributes> CompletableFuture<Result<T, A>> executeAsync(String extension,
                                                                                String operation,
                                                                                OperationParameters parameters) {
    OperationMessageProcessor processor = createProcessor(extension, operation, parameters);
    return from(processor.apply(just(getInitialiserEvent(muleContext))))
        .map(event -> Result.<T, A>builder(event.getMessage()).build())
        .toFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A extends Attributes> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {
    OperationMessageProcessor processor = createProcessor(extension, operation, params);
    Event process = processor.process(getInitialiserEvent(muleContext));
    return Result.<T, A>builder(process.getMessage()).build();
  }

  /**
   * Creates a new {@link OperationMessageProcessor} for the required operation and parses all the parameters passed by
   * the client user.
   */
  private OperationMessageProcessor createProcessor(String extensionName, String operationName, OperationParameters parameters) {
    ExtensionModel extension = findExtension(extensionName);
    OperationModel operation = findOperation(extension, operationName);
    ConfigurationProvider config = parameters.getConfigName().map(this::findConfiguration).orElse(null);
    Map<String, ValueResolver> resolvedParams = resolveParameters(parameters.get(), getInitialiserEvent(muleContext));
    try {
      OperationMessageProcessor processor = getOperationMessageProcessor(extension, operation, config, policyManager,
                                                                         resolvedParams, muleContext, "");
      processor.initialise();
      processor.start();
      return processor;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create Operation Message Processor"), e);
    }
  }

  private Map<String, ValueResolver> resolveParameters(Map<String, Object> parameters, Event event) {
    LinkedHashMap<String, ValueResolver> values = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType());
        resolveParameters(complex.getParameters(), event).forEach(builder::addPropertyResolver);
        try {
          values.put(name, new StaticValueResolver<>(builder.build(event)));
        } catch (MuleException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Could not construct parameter [%s]", name)), e);
        }
      } else {
        if (value instanceof String && parser.isContainsTemplate((String) value)) {
          values.put(name, new TypeSafeExpressionValueResolver<>((String) value, Object.class, muleContext));
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
}
