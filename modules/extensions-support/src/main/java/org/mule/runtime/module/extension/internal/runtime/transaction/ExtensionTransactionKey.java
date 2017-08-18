/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * The key used to bind a {@link ExtensionTransactionalResource} into a {@link Transaction}. Although logically speaking it is the
 * extension's {@link ConfigurationInstance} which should act as key, this class allows to decouple from its concrete type and
 * while not depending on its equals and hashCode implementations
 *
 * @since 4.0
 */
public class ExtensionTransactionKey {

  private final Reference<ConfigurationInstance> configReference;

  public ExtensionTransactionKey(ConfigurationInstance config) {
    configReference = new Reference<>(config);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ExtensionTransactionKey && configReference.equals(((ExtensionTransactionKey) obj).configReference);
  }

  @Override
  public int hashCode() {
    return configReference.hashCode();
  }
}
