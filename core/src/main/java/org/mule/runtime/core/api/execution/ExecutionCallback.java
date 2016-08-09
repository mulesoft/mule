/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
