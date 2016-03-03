/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.exception;

import org.mule.api.connection.ConnectionException;
import org.mule.extension.api.introspection.ExceptionEnrichableModel;
import org.mule.extension.api.introspection.ExceptionEnricher;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.util.ExceptionUtils;

import java.util.Optional;

/**
 * Given an {@link ExtensionModel} and another {@link ExceptionEnrichableModel} such
 * as {@link org.mule.extension.api.introspection.SourceModel} or {@link org.mule.extension.api.introspection.OperationModel}
 * this class will inspect for the correct {@link ExceptionEnricher} if there is one.
 *
 * It contains all the logic for operations and sources {@link Throwable} process and handling.
 *
 * @since 4.0
 */
public final class ExceptionEnricherManager
{

    private final ExceptionEnricher exceptionEnricher;

    public ExceptionEnricherManager(ExtensionModel extensionModel, ExceptionEnrichableModel childEnrichableModel)
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


    private ExceptionEnricher findExceptionEnricher(ExtensionModel extension, ExceptionEnrichableModel child)
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
