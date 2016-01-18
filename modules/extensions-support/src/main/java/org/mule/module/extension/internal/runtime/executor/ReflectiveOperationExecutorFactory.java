/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.executor;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.OperationExecutorFactory;

import java.lang.reflect.Method;

/**
 * An implementation of {@link OperationExecutorFactory} which produces instances
 * of {@link ReflectiveMethodOperationExecutor}.
 *
 * @param <T> the type of the class in which the implementing method is declared
 * @since 3.7.0
 */
public final class ReflectiveOperationExecutorFactory<T> implements OperationExecutorFactory
{

    private final Class<T> implementationClass;
    private final Method operationMethod;

    public ReflectiveOperationExecutorFactory(Class<T> implementationClass, Method operationMethod)
    {
        checkArgument(implementationClass != null, "implementationClass cannot be null");
        checkArgument(operationMethod != null, "operationMethod cannot be null");

        this.implementationClass = implementationClass;
        this.operationMethod = operationMethod;

    }

    @Override
    public OperationExecutor createExecutor()
    {
        Object delegate;
        try
        {
            delegate = implementationClass.newInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not create instance of operation class " + implementationClass.getName()), e);
        }

        return new ReflectiveMethodOperationExecutor(operationMethod, delegate);
    }
}
