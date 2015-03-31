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

/**
 * An {@link AbstractValidator} which checks that a
 * {@link #domain} is a valid top level domain
 *
 * @since 3.7.0
 */
public class TopLevelDomainValidator extends AbstractValidator
{

    private final String domain;

    public TopLevelDomainValidator(String domain, ValidationContext validationContext)
    {
        super(validationContext);
        this.domain = domain;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        return org.apache.commons.validator.routines.DomainValidator.getInstance().isValidTld(domain)
               ? ok()
               : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidTopLevelDomain(domain);
    }
}
