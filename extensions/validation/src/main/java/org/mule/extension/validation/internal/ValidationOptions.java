/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.extension.api.introspection.ExpressionSupport.LITERAL;
import org.mule.extension.annotation.api.Expression;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.validation.api.Validator;

/**
 * A class which groups parameters which configure a
 * {@link Validator} but are not the subject of the validation
 *
 * @since 3.7.0
 */
public final class ValidationOptions
{

    /**
     * Specifies the classname of the {@link Exception} to
     * be thrown if the validation fail. If it's not provided,
     * then the platform will choose a default type
     */
    @Parameter
    @Optional
    private String exceptionClass;

    /**
     * Specifies the message that is to be notified
     * to the user if the validation fails. It's marked
     * as not dynamic to allow eager evaluation of the expression
     * in case that the validation is successful and the message is not needed.
     * Components consuming this value are to manually check if this
     * is an expression and evaluate it in case that the validation failed
     */
    @Parameter
    @Expression(LITERAL)
    @Optional
    private String message = null;

    public String getExceptionClass()
    {
        return exceptionClass;
    }

    public String getMessage()
    {
        return message;
    }
}
