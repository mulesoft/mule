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

import org.apache.commons.validator.routines.CodeValidator;

/**
 * An {@link AbstractValidator} which tests a {@link #creditCardNumber}
 * to be valid for a given {@link #creditCardType}
 *
 * @since 3.7.0
 */
public class CreditCardNumberValidator extends AbstractValidator
{

    private final String creditCardNumber;
    private final CreditCardType creditCardType;

    public CreditCardNumberValidator(String creditCardNumber, CreditCardType creditCardType, ValidationContext validationContext)
    {
        super(validationContext);
        this.creditCardNumber = creditCardNumber;
        this.creditCardType = creditCardType;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        org.apache.commons.validator.routines.CreditCardValidator validator = new org.apache.commons.validator.routines.CreditCardValidator(new CodeValidator[] {creditCardType.getCodeValidator()});
        return validator.validate(creditCardNumber) != null ? ok() : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidCreditCard(creditCardNumber, creditCardType);
    }
}
