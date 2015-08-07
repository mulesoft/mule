/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import org.mule.api.MuleEvent;

/**
 * A factory for {@link Exception}s which represents
 * a validations which failed. Methods in this class
 * should always be invoked using a {@link ValidationResult}
 * object which {@@link ValidationResult#isError()} method
 * returns {@code true}
 *
 * @since 3.7.0
 */
public interface ExceptionFactory
{

    /**
     * Creates an exception of the given {@code exceptionClass}
     * which represents the given {@code result}.
     * The actual rules about what conditions is {@code exceptionClass}
     * expected to meet (e.g: presence of default constructor)
     * are up to the implementations.
     *
     * @param result         a {@link ValidationResult} which contains information about an error
     * @param exceptionClass the {@link Class} of the exception to be created
     * @param event          the {@link MuleEvent} on which validation failed
     * @param <T>            the type of the exception to be created
     * @return an {@link Exception} if type {@code T}
     */
    <T extends Exception> T createException(ValidationResult result, Class<T> exceptionClass, MuleEvent event);

    /**
     * Creates an exception of the given {@code exceptionClassName}
     * which represents the given {@code result}.
     * <p/>
     * The actual rules about what conditions is the exception {@link Class}
     * expected to meet (e.g: presence of default constructor)
     * are up to the implementations.
     *
     * @param result             a {@link ValidationResult} which contains information about an error
     * @param exceptionClassName the name of the exception {@link Class} to be thrown
     * @param event              the {@link MuleEvent} on which validation failed
     * @return a {@link Exception} of type {@code exceptionClassName}
     */
    Exception createException(ValidationResult result, String exceptionClassName, MuleEvent event);
}
