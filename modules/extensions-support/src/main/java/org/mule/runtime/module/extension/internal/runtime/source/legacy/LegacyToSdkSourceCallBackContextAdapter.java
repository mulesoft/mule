package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.notification.NotificationActionDefinition;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.tx.TransactionHandle;

import java.util.Optional;

public class LegacyToSdkSourceCallBackContextAdapter implements org.mule.sdk.api.runtime.source.SourceCallbackContext {

  private final SourceCallbackContext delegate;

  public LegacyToSdkSourceCallBackContextAdapter(SourceCallbackContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public TransactionHandle bindConnection(Object o) throws ConnectionException, TransactionException {
    return delegate.bindConnection(o);
  }

  @Override
  public <T> T getConnection() throws IllegalStateException {
    return delegate.getConnection();
  }

  @Override
  public TransactionHandle getTransactionHandle() {
    return delegate.getTransactionHandle();
  }

  @Override
  public boolean hasVariable(String s) {
    return delegate.hasVariable(s);
  }

  @Override
  public <T> Optional<T> getVariable(String s) {
    return delegate.getVariable(s);
  }

  @Override
  public void addVariable(String s, Object o) {
    delegate.addVariable(s, o);
  }

  @Override
  public void setCorrelationId(String s) {
    delegate.setCorrelationId(s);
  }

  @Override
  public Optional<String> getCorrelationId() {
    return delegate.getCorrelationId();
  }

  @Override
  public <T, A> SourceCallback<T, A> getSourceCallback() {
    return  new LegacySourceCallbackAdapter<>(delegate.getSourceCallback());
  }

  @Override
  public void fireOnHandle(NotificationActionDefinition<?> notificationActionDefinition, TypedValue<?> typedValue) {
    delegate.fireOnHandle(new SdkToLegacyNotificationActionDefinitionAdapter(notificationActionDefinition), typedValue);
  }
}
