/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.ValidationContext;
import org.mule.extension.validation.internal.ValidationMessages;

/**
 * Base class for all {@link Validator}s provided out of the box.
 * Because this module provides out of the box validators which contain
 * side functionality which is not directly expressed on the {@link Validator}
 * API (like i18n support), this base classes provides support for such logic
 */
abstract class AbstractValidator implements Validator
{

    // This field is transient so that it doesn't get logged when a successful validation is performed (the
    // validator instance fields are logged using reflection).
    private transient final ValidationContext validationContext;

    AbstractValidator(ValidationContext validationContext)
    {
        this.validationContext = validationContext;
    }

    /**
     * Returns the {@link ValidationMessages} instance to use
     *
     * @return a {@link ValidationMessages} instance
     */
    protected ValidationMessages getMessages()
    {
        return validationContext.getMessages();
    }

    /**
     * Implementations need to implement this method to return
     * the error message in case the validation failed
     *
     * @return a {@link Message}
     */
    protected abstract Message getDefaultErrorMessage();

    /**
     * Generates a {@link ValidationResult} which {@link ValidationResult#isError()}
     * method returns {@code true} and which message is the return value
     * of {@link #getDefaultErrorMessage()}. If the error messagee is an expression,
     * it will be evaluated before constructing the result object
     *
     * @return a {@link ValidationResult}
     */
    protected ValidationResult fail()
    {
        return error(getDefaultErrorMessage().getMessage());
    }
}
