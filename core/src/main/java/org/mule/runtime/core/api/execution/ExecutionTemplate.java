/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.execution;

import org.mule.api.annotation.NoImplement;

/**
 * ExecutionTemplate provides an execution context for message processing.
 *
 * Examples of execution context can be to provide error handling, transaction state verification, transactional demarcation.
 *
 * @param <T> type of the return value of the processing execution
 */
@NoImplement
public interface ExecutionTemplate<T> {

  T execute(ExecutionCallback<T> callback) throws Exception;
}
