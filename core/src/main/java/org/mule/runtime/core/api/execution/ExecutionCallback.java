/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.execution;


/**
 * Callback with logic to execute within a controlled environment provided by {@link ExecutionTemplate}
 *
 * @param <T> type of the return value of the processing execution
 */
public interface ExecutionCallback<T> {

  T process() throws Exception;
}
