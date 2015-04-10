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
import org.mule.transport.NullPayload;
import org.mule.util.ArrayUtils;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link AbstractValidator} which verifies that a given
 * {@link #value} is empty. The definition of empty depends on
 * the type of {@link #value}. If it's a {@link String} it will check that
 * it is not blank. If it's a {@link Collection}, array or {@link Map} it will check
 * that it's not empty. No other types are supported, an
 * {@link IllegalArgumentException} will be thrown if any other type is supplied
 *
 * @since 3.7.0
 */
public class EmptyValidator extends AbstractValidator
{

    private final Object value;
    private Message errorMessage;

    public EmptyValidator(Object value, ValidationContext validationContext)
    {
        super(validationContext);
        this.value = value;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        if (value == null || value instanceof NullPayload)
        {
            return ok();
        }
        else if (value instanceof String)
        {
            if (!StringUtils.isBlank((String) value))
            {
                errorMessage = getMessages().stringIsNotBlank();
                return fail();
            }
        }
        else if (value instanceof Collection)
        {
            if (!((Collection<?>) value).isEmpty())
            {
                errorMessage = getMessages().collectionIsNotEmpty();
                return fail();
            }
        }
        else if (value instanceof Map)
        {
            if (!((Map<?, ?>) value).isEmpty())
            {
                errorMessage = getMessages().mapIsNotEmpty();
                return fail();
            }
        }
        else if (value.getClass().isArray())
        {
            if (ArrayUtils.getLength(value) > 0)
            {
                errorMessage = getMessages().arrayIsNotEmpty();
                return fail();
            }
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format(
                            "Only instances of Map, Collection, Array and String can be checked for emptyness. Instance of %s was found instead",
                            value.getClass().getName()));
        }

        return ok();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return errorMessage;
    }
}
