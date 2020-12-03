package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.tx.TransactionHandle;

import java.util.Optional;

public class SdkToLegacySourceCallbackContextAdapter implements SourceCallbackContext {

  private final org.mule.sdk.api.runtime.source.SourceCallbackContext delegate;

  public SdkToLegacySourceCallbackContextAdapter(org.mule.sdk.api.runtime.source.SourceCallbackContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public TransactionHandle bindConnection(Object connection) throws ConnectionException, TransactionException {
    return new SdkToLegacyTransactionHandle(delegate.bindConnection(connection));
  }

  @Override
  public <T> T getConnection() throws IllegalStateException {
    return delegate.getConnection();
  }

  @Override
  public TransactionHandle getTransactionHandle() {
    return new SdkToLegacyTransactionHandle(delegate.getTransactionHandle());
  }

  @Override
  public boolean hasVariable(String variableName) {
    return delegate.hasVariable(variableName);
  }

  @Override
  public <T> Optional<T> getVariable(String variableName) {
    return delegate.getVariable(variableName);
  }

  @Override
  public void addVariable(String variableName, Object value) {
    delegate.addVariable(variableName, value);
  }

  @Override
  public void setCorrelationId(String correlationId) {
    delegate.setCorrelationId(correlationId);
  }

  @Override
  public Optional<String> getCorrelationId() {
    return delegate.getCorrelationId();
  }

  @Override
  public <T, A> SourceCallback<T, A> getSourceCallback() {
    return new SdkToLegacySourceCallbackAdapter<>(delegate.getSourceCallback());
  }

  @Override
  public void fireOnHandle(NotificationActionDefinition<?> action, TypedValue<?> data) {
    delegate.fireOnHandle(action, data);
  }
}
