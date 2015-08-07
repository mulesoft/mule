/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import java.util.List;

/**
 * A specialization of {@link ValidationResult} which
 * represents a group of validations that were executed together.
 * <p/>
 * This interface redefines the {@link ValidationResult} contract
 * so that the {@link #isError()} and {@link #getMessage()} methods
 * consider the results of all the validations.
 * <p/>
 * Additionally, the {@link #getFailedValidationResults()} method
 * is added to give more detailed access to the validations that failed
 *
 * @since 3.7.0
 */
public interface MultipleValidationResult extends ValidationResult
{

    /**
     * @return {@code true} if at least one of the {@link #getFailedValidationResults()} is not empty. {@code false} otherwise
     */
    @Override
    boolean isError();

    /**
     * Returns all the messages from the {@link #getFailedValidationResults()} which failed
     * separated by a {@code \n} character
     */
    @Override
    String getMessage();

    /**
     * The {@link ValidationResult}s which {@link ValidationResult#isError()} method
     * returns {@code false}
     *
     * @return an immutable view of the successful {@link ValidationResult}
     */
    List<ValidationResult> getFailedValidationResults();
}
