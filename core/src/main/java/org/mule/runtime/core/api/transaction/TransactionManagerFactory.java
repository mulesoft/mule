/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.config.MuleConfiguration;

import javax.transaction.TransactionManager;

/**
 * <code>TransactionManagerFactory</code> is a factory class for creating a transaction manager for the Mule container.
 * 
 */
@NoImplement
public interface TransactionManagerFactory {

  /**
   * Creates of obtains the jta transaction manager to use for mule transactions
   * 
   * @return the transaction manager to use
   * @throws Exception if the transaction manager cannot be located or created
   * @param config Mule configuration parameters
   */
  TransactionManager create(MuleConfiguration config) throws Exception;
}
