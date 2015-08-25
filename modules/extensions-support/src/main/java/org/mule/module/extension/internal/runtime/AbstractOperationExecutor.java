/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;

import java.util.function.Consumer;

/**
 * Base class for {@link OperationExecutor}s which handles common behaviors such as notifications, etc.
 *
 * @since 4.0
 */
public abstract class AbstractOperationExecutor implements OperationExecutor
{

    /**
     * Executes the operation by delegating into {@link #doExecute(OperationContext)}.
     * <p/>
     * If {@code operationContext} is an instance of {@link OperationContextAdapter},
     * then {@link OperationContextAdapter#notifySuccessfulOperation(Object)} or
     * {@link OperationContextAdapter#notifyFailedOperation(Exception)} is invoked depending on the
     * execution being successful or not.
     *
     * @param operationContext a {@link OperationContext} with information about the execution
     * @return the result object
     * @throws Exception if anything goes wrong
     */
    @Override
    public final Object execute(OperationContext operationContext) throws Exception
    {
        Object result;
        try
        {
            result = doExecute(operationContext);
            notify(operationContext, adapter -> adapter.notifySuccessfulOperation(result));
        }
        catch (Exception e)
        {
            notify(operationContext, adapter -> adapter.notifyFailedOperation(e));
            throw e;
        }

        return result;
    }

    /**
     * Executes the actual operation logic.
     *
     * @param operationContext the operation's context
     * @return the operation's result
     * @throws Exception if anything goes wrong
     */
    protected abstract Object doExecute(OperationContext operationContext) throws Exception;

    private void notify(OperationContext operationContext, Consumer<OperationContextAdapter> notifier)
    {
        if (operationContext instanceof OperationContextAdapter)
        {
            notifier.accept((OperationContextAdapter) operationContext);
        }
    }
}
