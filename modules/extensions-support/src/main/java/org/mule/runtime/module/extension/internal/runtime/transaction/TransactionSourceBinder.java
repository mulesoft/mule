/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Optional;

import javax.transaction.TransactionManager;

/**
 * Helper class in charge of bind the Source connection to the current Transaction, if one is available.
 * Also, if the Transaction
 *
 * @since 4.0
 */
public class TransactionSourceBinder {

  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;
  private final String applicationName;
  private final NotificationDispatcher notificationDispatcher;
  private final SingleResourceTransactionFactoryManager transactionFactoryManager;
  private final TransactionBindingDelegate transactionBindingDelegate;

  public TransactionSourceBinder(ExtensionModel extensionModel, ComponentModel componentModel, String applicationName,
                                 NotificationDispatcher notificationDispatcher,
                                 SingleResourceTransactionFactoryManager transactionFactoryManager) {
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.applicationName = applicationName;
    this.notificationDispatcher = notificationDispatcher;
    this.transactionFactoryManager = transactionFactoryManager;
    transactionBindingDelegate = new TransactionBindingDelegate(extensionModel, componentModel);
  }

  public <T extends TransactionalConnection> Optional<ConnectionHandler<T>> bindToTransaction(TransactionConfig transactionConfig,
                                                                                              ConfigurationInstance configurationInstance,
                                                                                              ComponentLocation sourceLocation,
                                                                                              ConnectionHandler connectionHandler,
                                                                                              TransactionManager transactionManager,
                                                                                              int timeout)
      throws ConnectionException, TransactionException {

    if (!transactionConfig.isTransacted()) {
      return empty();
    }

    Transaction tx =
        transactionConfig.getFactory().beginTransaction(applicationName, notificationDispatcher, transactionFactoryManager,
                                                        transactionManager, timeout);
    ((TransactionAdapter) tx).setComponentLocation(sourceLocation);

    ConfigurationInstance configuration = ofNullable(configurationInstance)
        .orElseThrow(() -> new IllegalStateException(format(
                                                            "Source '%s' of extension '%s' cannot participate in a transaction because it doesn't have a config",
                                                            componentModel.getName(),
                                                            extensionModel.getName())));

    final ExtensionTransactionKey txKey = new ExtensionTransactionKey(configuration);
    return Optional.of(transactionBindingDelegate.getBoundResource(transactionConfig, txKey, () -> connectionHandler));
  }
}
