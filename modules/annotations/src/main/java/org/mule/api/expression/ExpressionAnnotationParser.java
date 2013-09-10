/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.expression;

import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

/**
 * TODO
 */
public interface ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType);

    public boolean supports(Annotation annotation);
}
