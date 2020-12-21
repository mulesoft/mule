/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.source.SourceCallbackContextAdapter;
import org.mule.sdk.api.notification.NotificationActionDefinition;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.tx.TransactionHandle;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LegacyToSdkSourceCallBackContextAdapter implements SourceCallbackContextAdapter {

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
    return new LegacySourceCallbackAdapter<>(delegate.getSourceCallback());
  }

  @Override
  public void fireOnHandle(NotificationActionDefinition<?> notificationActionDefinition, TypedValue<?> typedValue) {
    delegate.fireOnHandle(new SdkToLegacyNotificationActionDefinitionAdapter(notificationActionDefinition), typedValue);
  }

  //RESOLVE THIS

  @Override
  public void releaseConnection() {
    if (delegate instanceof LegacySourceCallbackContextAdapter) {
      ((LegacySourceCallbackContextAdapter) delegate).releaseConnection();
    }
  }

  @Override
  public void dispatched() {
    if (delegate instanceof LegacySourceCallbackContextAdapter) {
      ((LegacySourceCallbackContextAdapter) delegate).dispatched();
    }
  }

  @Override
  public List<NotificationFunction> getNotificationsFunctions() {
    if (delegate instanceof LegacySourceCallbackContextAdapter) {
      return ((LegacySourceCallbackContextAdapter) delegate).getNotificationsFunctions();
    }
    return emptyList();
  }
}
