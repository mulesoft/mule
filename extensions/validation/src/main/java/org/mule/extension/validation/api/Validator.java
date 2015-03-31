/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import org.mule.api.MuleEvent;

/**
 * A component which performs a validation and expresses
 * its result through a {@link ValidationResult} object.
 * <p/>
 * The {@link #validate(MuleEvent)} method receives a
 * {@link MuleEvent} in order to make it generic and easy to extend.
 * However, that doesn't necessarily mean that all the validation
 * is performed with or over the event exclusively. Thread
 * safeness is not to be assumed over instances of this class
 * since the validator could be stateful.
 *
 * @since 3.7.0
 */
public interface Validator
{

    /**
     * Performs the validation and generates a
     * {@link ValidationResult} back.
     *
     * @param event the current {@link MuleEvent}
     * @return a {@link ValidationResult}
     */
    ValidationResult validate(MuleEvent event);
}
