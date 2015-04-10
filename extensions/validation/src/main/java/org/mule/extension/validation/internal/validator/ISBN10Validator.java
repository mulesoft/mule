/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

import org.apache.commons.validator.routines.ISBNValidator;

/**
 * A {@link AbstractValidator} which checks that a
 * {@link #isbn} code is a valid ISBN10 one
 *
 * @since 3.7.0
 */
public class ISBN10Validator extends AbstractValidator
{

    private final String isbn;

    public ISBN10Validator(String isbn, ValidationContext validationContext)
    {
        super(validationContext);
        this.isbn = isbn;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        return ISBNValidator.getInstance().isValidISBN10(isbn) ? ok() : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidISBN10(isbn);
    }
}
