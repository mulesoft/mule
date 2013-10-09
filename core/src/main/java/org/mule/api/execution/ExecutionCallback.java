/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.execution;


/**
 * Callback with logic to execute within a controlled environment provided by {@link ExecutionTemplate}
 *
 * @param <T> type of the return value of the processing execution
 */
public interface ExecutionCallback<T>
{
    T process() throws Exception;
}
