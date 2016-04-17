/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.extension.api.introspection.exception.ExceptionEnrichableModel;
import org.mule.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.core.util.ExceptionUtils;

import java.util.Optional;

/**
 * Given a {@link RuntimeExtensionModel} and another {@link ExceptionEnrichableModel} such
 * as {@link RuntimeSourceModel} or {@link RuntimeOperationModel},
 * this class will inspect for the correct {@link ExceptionEnricher} if there is one.
 * <p>
 * It contains all the logic for operations and sources {@link Throwable} process and handling.
 *
 * @since 4.0
 */
public final class ExceptionEnricherManager
{

    private final ExceptionEnricher exceptionEnricher;

    public ExceptionEnricherManager(RuntimeExtensionModel extensionModel, ExceptionEnrichableModel childEnrichableModel)
    {
        exceptionEnricher = findExceptionEnricher(extensionModel, childEnrichableModel);
    }

    public Exception processException(Throwable t)
    {
        Exception root = handleException(t);
        Exception exception = exceptionEnricher.enrichException(root);
        return exception != null ? exception : root;
    }

    public Exception handleException(Throwable e)
    {
        Throwable root;
        Optional<ConnectionException> connectionException = ExceptionUtils.extractRootConnectionException(e);
        if (connectionException.isPresent())
        {
            root = connectionException.get();
        }
        else
        {
            root = ExceptionUtils.getRootCause(e);
            if (root == null)
            {
                root = e;
            }
        }
        return wrapInException(root);
    }

    private Exception wrapInException(Throwable t)
    {
        return t instanceof Exception ? (Exception) t : new Exception(t);
    }


    private ExceptionEnricher findExceptionEnricher(RuntimeExtensionModel extension, ExceptionEnrichableModel child)
    {
        Optional<ExceptionEnricherFactory> exceptionEnricherFactory = child.getExceptionEnricherFactory();
        if (!exceptionEnricherFactory.isPresent())
        {
            exceptionEnricherFactory = extension.getExceptionEnricherFactory();
        }
        return exceptionEnricherFactory.isPresent() ? exceptionEnricherFactory.get().createEnricher() : new NullExceptionEnricher();
    }


    public ExceptionEnricher getExceptionEnricher()
    {
        return exceptionEnricher;
    }

}
