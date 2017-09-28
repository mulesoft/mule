/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.Optional;

/**
 * {@link ConnectionProviderWrapper} implementation which handles the exceptions occurred when {@link #connect()} and the
 * failed {@link ConnectionValidationResult} from the {@link #validate(Object)}, this wrapper consumes these outputs and if a
 * {@link ErrorTypeDefinition} is provided transforms it and communicates the proper {@link ErrorType}
 *
 * @param <C>
 * @since 4.0
 */
public final class ErrorTypeHandlerConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

  private final ErrorTypeRepository errorTypeRepository;
  private final String prefix;
  private final ReconnectionConfig reconnectionConfig;

  public ErrorTypeHandlerConnectionProviderWrapper(ConnectionProvider<C> connectionProvider,
                                                   ExtensionModel extensionModel,
                                                   ReconnectionConfig reconnectionConfig,
                                                   MuleContext muleContext) {
    super(connectionProvider);
    this.errorTypeRepository = muleContext.getErrorTypeRepository();
    this.prefix = extensionModel.getXmlDslModel().getPrefix().toUpperCase();
    this.reconnectionConfig = reconnectionConfig;
  }

  /**
   * Delegates to the proper {@link ConnectionProvider}, if this fails and throws a {@link ConnectionException}, this method will
   * introspect into the cause of this exception and if the cause is a {@link ModuleException} a new {@link ConnectionException}
   * will be created communicating the proper {@link ErrorType}, otherwise the original exception will be propagated.
   * 
   * @return a ready to use {@code Connection}
   * @throws ConnectionException when a problem occurs creating the connection
   */
  @Override
  public C connect() throws ConnectionException {
    try {
      return getDelegate().connect();
    } catch (ConnectionException e) {
      Throwable cause = e.getCause();
      throw getErrorType(cause)
          .map(errorType -> e.getClass().equals(ConnectionException.class)
              ? new ConnectionException(e.getMessage(), e.getCause(), errorType)
              : new ConnectionException(e.getMessage(), e, errorType))
          .orElse(e);
    }
  }

  /**
   * Delegates the validation of the connection {@link C} to the proper {@link ConnectionProvider}, is the validation is not valid
   * this method will introspect into the exception cause and look for a {@link ModuleException}, if one is found a new
   * {@link ConnectionValidationResult} will be created with the same information, but communicating the {@link ErrorType}.
   * 
   * @param connection a non {@code null} {@link C}.
   * @return a {@link ConnectionValidationResult} indicating if the connection is valid or not.
   */
  @Override
  public ConnectionValidationResult validate(C connection) {
    ConnectionValidationResult originalResult = getDelegate().validate(connection);
    if (originalResult.isValid()) {
      return originalResult;
    } else {
      Exception exception = originalResult.getException();
      return getErrorType(exception)
          .map(type -> failure(originalResult.getMessage(), type, exception))
          .orElse(originalResult);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    final ConnectionProvider<C> delegate = getDelegate();
    return delegate instanceof ConnectionProviderWrapper
        ? ((ConnectionProviderWrapper) delegate).getRetryPolicyTemplate()
        : super.getRetryPolicyTemplate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    final ConnectionProvider<C> delegate = getDelegate();
    return delegate instanceof ConnectionProviderWrapper
        ? ((ConnectionProviderWrapper) delegate).getReconnectionConfig()
        : ofNullable(reconnectionConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    ConnectionProvider<C> delegate = getDelegate();
    return delegate instanceof ConnectionProviderWrapper
        ? ((ConnectionProviderWrapper) delegate).getPoolingProfile()
        : empty();
  }

  private Optional<ErrorType> getErrorType(Throwable exception) {
    if (exception instanceof ModuleException) {
      return getErrorType(((ModuleException) exception).getType());
    } else {
      return exception != null && exception.getCause() != null
          ? getErrorType(exception.getCause())
          : empty();
    }
  }

  private Optional<ErrorType> getErrorType(ErrorTypeDefinition errorType) {
    return errorTypeRepository.getErrorType(getIdentifier(errorType));
  }

  private ComponentIdentifier getIdentifier(ErrorTypeDefinition errorType) {
    return builder().name(errorType.getType()).namespace(prefix).build();
  }
}
