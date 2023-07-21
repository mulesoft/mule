/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

/**
 * A factory than can create both bound and unbound transactions
 */
public interface UniversalTransactionFactory extends TransactionFactory, UnboundTransactionFactory {
}
