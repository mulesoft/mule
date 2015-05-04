/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extension.runtime.OperationContext;

/**
 * An implementation of {@link ReturnDelegate} intended
 * for operations which return {@link Void} and that
 * were executed with a {@link OperationContextAdapter}
 * <p/>
 * It returns the {@link MuleEvent} that {@link OperationContextAdapter}
 * provides. Notices that this class will fail if used with any other type
 * of {@link OperationContext}
 * <p/>
 * This class is intended to be used as a singleton, use the
 * {@link #INSTANCE} attribute to access the instance
 *
 * @since 3.7.0
 */
final class VoidReturnDelegate implements ReturnDelegate
{

    static final ReturnDelegate INSTANCE = new VoidReturnDelegate();

    private VoidReturnDelegate()
    {
    }

    /**
     * {@inheritDoc}
     * @return {@link OperationContextAdapter#getEvent()}
     */
    @Override
    public Object asReturnValue(Object value, OperationContext operationContext)
    {
        return ((OperationContextAdapter) operationContext).getEvent();
    }
}
