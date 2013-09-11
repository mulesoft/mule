/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on component methods, this annotation marks the method parameter that will be used to pass in one or more of the headers received.
 * This annotation value can define a single header, a comma-separated list of header names, '*' to denote all headers, or a comma-separated list
 * of wildcard expressions such as 'MULE_*, X-*'. By default, if a named header is not present on the current message, an exception will be thrown.
 * However, if the header name is defined with the '?' post fix, it will be marked as optional.
 * <p/>
 * When defining multiple header names or using wildcards, the parameter can be a {@link java.util.Map} or {@link java.util.List}. If a
 * {@link java.util.Map} is used, the header name and value is passed in. If {@link java.util.List} is used, just the header values are used.
 * If a single header name is defined, the header type can be used as the parameter type, though {@link java.util.List} or {@link java.util.Map}
 * can be used too.
 *
 * The Inbound headers collection is immutable, so the headers Map or List passed in will be immutable too. Attempting to write to the Map or List will result in an {@link UnsupportedOperationException}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("messageProperty")
public @interface InboundHeaders
{
    /**
     * Defines the headers that should be injected into the parameter. This can be a single header, a comma-separated
     * list of header names,'*' to denote all headers or a comma-separated list of wildcard expressions. By default,
     * if a named header is not present, an exception will be thrown. However, if the header name is defined with the
     * '?' post fix, it will be marked as optional.
     * The optional '?' post fix is not supported when using wildcard expressions
     *
     * @return the header expression used to query the message for headers
     */
     String value();
}
