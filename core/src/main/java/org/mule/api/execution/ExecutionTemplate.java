/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.execution;

/**
 * ExecutionTemplate provides an execution context for message processing.
 *
 * Examples of execution context can be to provide error handling, transaction state verification,
 * transactional demarcation.
 *
 * @param <T> type of the return value of the processing execution
 */
public interface ExecutionTemplate<T>
{
    public T execute(ExecutionCallback<T> callback) throws Exception;
}
