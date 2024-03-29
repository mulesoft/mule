/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
