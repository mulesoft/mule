/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.api.MultipleValidationResult;
import org.mule.extension.validation.api.ValidationResult;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * A immutable implementation of {@link MultipleValidationResult}.
 * <p/>
 * Instances are to be created using the {@link #of(Iterable)} factory method
 *
 * @since 3.7.0
 */
public final class ImmutableMultipleValidationResult implements MultipleValidationResult
{

    private final List<ValidationResult> failedResults;
    private final boolean error;
    private final String message;

    /**
     * A {@link Iterable} with all the {@link ValidationResult} that were generated
     * together, both failed and successful alike.
     *
     * @param results the obtained {@link ValidationResult} objects
     * @return a {@link MultipleValidationResult}
     */
    public static MultipleValidationResult of(Iterable<ValidationResult> results)
    {
        ImmutableList.Builder<ValidationResult> failedResultsBuilder = ImmutableList.builder();
        StringBuilder message = new StringBuilder();
        boolean error = false;

        for (ValidationResult result : results)
        {
            if (result.isError())
            {
                failedResultsBuilder.add(result);
                if (message.length() > 0)
                {
                    message.append('\n');
                }

                message.append(result.getMessage());
                error = true;
            }
        }

        return new ImmutableMultipleValidationResult(failedResultsBuilder.build(), error, message.toString());
    }

    private ImmutableMultipleValidationResult(List<ValidationResult> failedResults, boolean error, String message)
    {
        this.failedResults = failedResults;
        this.error = error;
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError()
    {
        return error;
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
    public List<ValidationResult> getFailedValidationResults()
    {
        return failedResults;
    }
}
