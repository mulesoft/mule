/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

/**
 * An {@link AbstractValidator} which verifies that a {@link #countryCode}
 * is a valid domain top level domain
 *
 * @since 3.7.0
 */
public class DomainCountryCodeValidator extends AbstractValidator
{

    private final String countryCode;

    public DomainCountryCodeValidator(String countryCode, ValidationContext validationContext)
    {
        super(validationContext);
        this.countryCode = countryCode;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        return org.apache.commons.validator.routines.DomainValidator.getInstance().isValidCountryCodeTld(countryCode)
               ? ok()
               : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidDomainCountryCode(countryCode);
    }
}
