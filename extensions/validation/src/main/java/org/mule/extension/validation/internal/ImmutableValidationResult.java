/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;

/**
 * An immutable implementation of {@link ValidationResult}.
 * It provides a series of static factory methods
 * for creating a result in which the validation succeeded
 * ({@link #ok()}), and other two for validations that failed
 * ({@link #error(Message)} and {@link #error(String)}).
 *
 * @since 3.7.0
 */
public class ImmutableValidationResult implements ValidationResult
{

    /**
     * Since this class is immutable, we can always use the same instance
     * for results which represent a successful validation
     */
    private static final ValidationResult OK = new ImmutableValidationResult(EMPTY, false);

    private final String message;
    private final boolean error;

    /**
     * Creates a new instance with the given {@code message}
     * and which {@link #isError()} returns {@code true}
     *
     * @param message a message
     * @return a new instance of {@link ImmutableValidationResult}
     */
    public static ValidationResult error(String message)
    {
        return new ImmutableValidationResult(message, true);
    }

    /**
     * Creates a new instance with the given {@code message}
     * and which {@link #isError()} returns {@code true}
     *
     * @param message a message
     * @return a new instance of {@link ImmutableValidationResult}
     */
    public static ValidationResult error(Message message)
    {
        return error(message.getMessage());
    }

    /**
     * returns a {@link ImmutableValidationResult} without message and
     * which {@link #isError()} method returns {@code false}. Since this
     * class is immutable, the same instance is always returned
     *
     * @return {@link #OK}
     */
    public static ValidationResult ok()
    {
        return OK;
    }

    private ImmutableValidationResult(String message, boolean error)
    {
        this.message = message;
        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage()
    {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError()
    {
        return error;
    }
}
