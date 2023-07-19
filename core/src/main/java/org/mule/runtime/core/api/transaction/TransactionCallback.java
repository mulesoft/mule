/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

public interface TransactionCallback<T> {

  T doInTransaction() throws Exception;
}
