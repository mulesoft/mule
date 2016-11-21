/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isTransactional;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toActionCode;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ExecutionContextAdapter} which adds additional information which is relevant to this
 * implementation of the extensions-api, even though it's not part of the API itself
 *
 * @since 3.7.0
 */
public class DefaultExecutionContext<M extends ComponentModel> implements ExecutionContextAdapter<M> {

  private static final ExtensionTransactionFactory TRANSACTION_FACTORY = new ExtensionTransactionFactory();

  private final ExtensionModel extensionModel;
  private final Optional<ConfigurationInstance> configuration;
  private final Map<String, Object> parameters;
  private final Map<String, Object> variables = new HashMap<>();
  private final M componentModel;
  private final MuleContext muleContext;
  private Event event;
  private Optional<TransactionConfig> transactionConfig = null;
  private Supplier<Optional<TransactionConfig>> transactionConfigSupplier;

  /**
   * Creates a new instance with the given state
   *
   * @param configuration the {@link ConfigurationInstance} that the operation will use
   * @param parameters the parameters that the operation will use
   * @param componentModel the {@link ComponentModel} for the component being executed
   * @param event the current {@link Event}
   */
  public DefaultExecutionContext(ExtensionModel extensionModel,
                                 Optional<ConfigurationInstance> configuration,
                                 Map<String, Object> parameters,
                                 M componentModel,
                                 Event event,
                                 MuleContext muleContext) {

    this.extensionModel = extensionModel;
    this.configuration = configuration;
    this.event = event;
    this.componentModel = componentModel;
    this.parameters = parameters;
    this.muleContext = muleContext;
    transactionConfigSupplier = () -> {
      synchronized (this) {
        if (transactionConfig == null) {
          transactionConfig = isTransactional(componentModel) ? Optional.of(buildTransactionConfig()) : empty();

          transactionConfigSupplier = () -> transactionConfig;
        }
        return transactionConfig;
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return configuration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasParameter(String parameterName) {
    return parameters.containsKey(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getParameter(String parameterName) {
    if (hasParameter(parameterName)) {
      return (T) parameters.get(parameterName);
    } else {
      throw new NoSuchElementException(parameterName);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getTypeSafeParameter(String parameterName, Class<? extends T> expectedType) {
    Object value = getParameter(parameterName);
    if (value == null) {
      return null;
    }

    if (!expectedType.isInstance(value)) {
      throw new IllegalArgumentException(String.format("'%s' was expected to be of type '%s' but type '%s' was found instead",
                                                       parameterName, expectedType.getName(), value.getClass().getName()));
    }

    return (T) value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getVariable(String key) {
    return (T) variables.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object setVariable(String key, Object value) {
    checkArgument(key != null, "null keys are not allowed");
    checkArgument(value != null, "null values are not allowed");
    return variables.put(key, value);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T removeVariable(String key) {
    checkArgument(key != null, "null keys are not allowed");
    return (T) variables.remove(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Event getEvent() {
    return event;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public M getComponentModel() {
    return componentModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * {@inheritDoc}
   */
  public Optional<TransactionConfig> getTransactionConfig() {
    return transactionConfigSupplier.get();
  }

  private TransactionConfig buildTransactionConfig() {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setAction(toActionCode(getTransactionalAction()));
    transactionConfig.setMuleContext(muleContext);
    transactionConfig.setFactory(TRANSACTION_FACTORY);

    return transactionConfig;
  }

  private OperationTransactionalAction getTransactionalAction() {
    OperationTransactionalAction action =
        getTypeSafeParameter(TRANSACTIONAL_ACTION_PARAMETER_NAME, OperationTransactionalAction.class);
    if (action == null) {
      throw new IllegalArgumentException(format("Operation '%s' from extension '%s' is transactional but no transactional action defined",
                                                componentModel.getName(),
                                                extensionModel.getName()));
    }

    return action;
  }
}
