/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;
import org.mule.util.ArrayUtils;

import java.util.Collection;
import java.util.Map;

/**
 * An {@link AbstractValidator} which verifies that {@link #value} has a size between certain inclusive boundaries. This
 * validator is capable of handling instances of {@link String}, {@link Collection},
 * {@link Map} and arrays
 *
 * @since 3.7.0
 */
public class SizeValidator extends AbstractValidator
{

    private final Object value;
    private final int minSize;
    private final Integer maxSize;

    private Message errorMessage;

    public SizeValidator(Object value, int minSize, Integer maxSize, ValidationContext validationContext)
    {
        super(validationContext);
        this.value = value;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        int inputLength = getSize(value);
        if (inputLength < minSize)
        {
            errorMessage = getMessages().lowerThanMinSize(value, minSize, inputLength);
            return fail();
        }

        if (maxSize != null && inputLength > maxSize)
        {
            errorMessage = getMessages().greaterThanMaxSize(value, maxSize, inputLength);
            return fail();
        }

        return ok();
    }

    private int getSize(Object value)
    {
        checkArgument(value != null, "Cannot check size of a null value");
        if (value instanceof String)
        {
            return ((String) value).length();
        }
        else if (value instanceof Collection)
        {
            return ((Collection<?>) value).size();
        }
        else if (value instanceof Map)
        {
            return ((Map<?, ?>) value).size();
        }
        else if (value.getClass().isArray())
        {
            return ArrayUtils.getLength(value);
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format(
                            "Only instances of Map, Collection, Array and String can be checked for size. Instance of %s was found instead",
                            value.getClass().getName()));
        }
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return errorMessage;
    }
}
