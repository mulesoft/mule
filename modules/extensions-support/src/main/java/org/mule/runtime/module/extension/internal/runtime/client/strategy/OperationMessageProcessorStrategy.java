/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;

public abstract class OperationMessageProcessorStrategy {

  protected final TemplateParser parser = TemplateParser.createMuleStyleParser();

  protected ExtensionManager extensionManager;
  protected Registry registry;
  protected MuleContext muleContext;
  protected ReflectionCache reflectionCache;
  protected PolicyManager policyManager;
  protected CoreEvent event;



  public OperationMessageProcessorStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                           PolicyManager policyManager, ReflectionCache reflectionCache, CoreEvent event) {
    this.extensionManager = extensionManager;
    this.registry = registry;
    this.muleContext = muleContext;
    this.policyManager = policyManager;
    this.reflectionCache = reflectionCache;
    this.event = event;
  }

  public abstract OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                         OperationParameters parameters);

  public abstract CoreEvent getEvent(OperationParameters parameters);

  public abstract void disposeProcessor(OperationMessageProcessor operationMessageProcessor);

  protected OperationMessageProcessor createProcessor(String extensionName, String operationName, Optional<String> configName,
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

  protected OperationModel findOperation(ExtensionModel extensionModel, String operationName) {
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

  protected ConfigurationProvider findConfiguration(String configName) {
    return extensionManager.getConfigurationProvider(configName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configName + "] found")));
  }

  protected ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }

  protected void doDisposeProcessor(OperationMessageProcessor processor) {
    if (processor == null) {
      return;
    }
    try {
      processor.stop();
      processor.dispose();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while disposing the executing operation"), e);
    }
  }

  protected CoreEvent getBaseEvent() {
    return event == null ? getInitialiserEvent() : event;
  }

}
