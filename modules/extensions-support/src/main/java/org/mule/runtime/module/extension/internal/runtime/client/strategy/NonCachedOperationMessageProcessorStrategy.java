/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;

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
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.LinkedHashMap;
import java.util.Map;

public class NonCachedOperationMessageProcessorStrategy extends OperationMessageProcessorStrategy {

  public NonCachedOperationMessageProcessorStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                                    PolicyManager policyManager, ReflectionCache reflectionCache,
                                                    CoreEvent event) {
    super(extensionManager, registry, muleContext, policyManager, reflectionCache, event);
  }

  @Override
  public OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                OperationParameters parameters) {
    return createProcessor(extensionName, operationName, parameters.getConfigName(), resolveParameters(parameters.get(), getEvent(parameters)));
  }

  private Map<String, ValueResolver> resolveParameters(Map<String, Object> parameters, CoreEvent event) {
    LinkedHashMap<String, ValueResolver> values = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType(), reflectionCache);
        resolveParameters(complex.getParameters(), event).forEach((propertyName, valueResolver) -> {
          try {
            initialiseIfNeeded(valueResolver, true, muleContext);
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


  @Override
  public CoreEvent getEvent(OperationParameters parameters) {
    return getBaseEvent();
  }

  @Override
  public void disposeProcessor(OperationMessageProcessor operationMessageProcessor) {
    doDisposeProcessor(operationMessageProcessor);
  }
}
