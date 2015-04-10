/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import org.mule.api.MuleEvent;

/**
 * A specialization of {@link ValidationResult} which
 * takes a {@link MultipleValidationResult} as a result.
 *
 * @since 3.7.0
 */
public final class MultipleValidationException extends ValidationException
{

    private final MultipleValidationResult multipleValidationResult;

    public MultipleValidationException(MultipleValidationResult multipleValidationResult, MuleEvent event)
    {
        super(multipleValidationResult, event);
        this.multipleValidationResult = multipleValidationResult;
    }

    /**
     * The {@link MultipleValidationResult} which this exception informs
     * @return a {@link MultipleValidationResult}
     */
    public MultipleValidationResult getMultipleValidationResult()
    {
        return multipleValidationResult;
    }
}
