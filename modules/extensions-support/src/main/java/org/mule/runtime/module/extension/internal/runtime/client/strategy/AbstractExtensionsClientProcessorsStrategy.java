/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findOperation;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.util.TemplateParser;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class that hold useful logic to implement a {@link ExtensionsClientProcessorsStrategy}
 *
 * @since 4.1.6
 */
public abstract class AbstractExtensionsClientProcessorsStrategy implements ExtensionsClientProcessorsStrategy {

  protected final TemplateParser parser = TemplateParser.createMuleStyleParser();

  protected final ExtensionManager extensionManager;
  protected final Registry registry;
  protected final MuleContext muleContext;
  protected final ReflectionCache reflectionCache;
  protected final PolicyManager policyManager;
  protected final CoreEvent event;


  /**
   * Creates a new instance
   */
  public AbstractExtensionsClientProcessorsStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                                    PolicyManager policyManager, ReflectionCache reflectionCache,
                                                    CoreEvent event) {
    this.extensionManager = extensionManager;
    this.registry = registry;
    this.muleContext = muleContext;
    this.policyManager = policyManager;
    this.reflectionCache = reflectionCache;
    this.event = event;
  }

  protected OperationMessageProcessor createProcessor(String extensionName, String operationName, Optional<String> configName,
                                                      Map<String, ValueResolver> parameters) {
    ExtensionModel extension = findExtension(extensionName);
    OperationModel operation = findOperation(extension, operationName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Operation [" + operationName + "] Found")));
    ConfigurationProvider config = configName.map(this::findConfiguration).orElse(null);
    Map<String, ValueResolver> resolvedParams = parameters;
    try {
      OperationMessageProcessor processor =
          new OperationMessageProcessorBuilder(extension, operation, emptyList(), policyManager, muleContext, registry)
              .setConfigurationProvider(config)
              .setParameters(resolvedParams)
              .setTerminationTimeout(-1)
              .build();

      initialiseIfNeeded(processor, muleContext);
      processor.start();
      return processor;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create Operation Message Processor"), e);
    }
  }

  protected ConfigurationProvider findConfiguration(String configName) {
    return extensionManager.getConfigurationProvider(configName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configName + "] found")));
  }

  protected ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }

  protected Map<String, ValueResolver> resolveParameters(Map<String, Object> parameters, CoreEvent event) {
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
          valueResolver = new StaticValueResolver<>(builder.build(ValueResolvingContext.builder(event).build()));
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

  protected CoreEvent getBaseEvent() {
    return event == null ? getInitialiserEvent() : event;
  }

}
