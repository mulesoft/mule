/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.expression;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This allows Groovy codes to be executed on the current message.
 *
 * Mule will bind a number of objects to the groovy context:
 *
 * <ul>
 *  <li>muleContext - A reference to the MuleContext object.</li>
 *  <li>eventContext - A reference to the event context. This allows you to dispatch events progammatically from your script.</li>
 *  <li>message - The current message.</li>
 *  <li>payload - The payload of the current message. This is just a shortcut to $message.payload.</li>
 *  <li>service - A reference to the current service object.</li>
 *  <li>id - The current event ID. This is a UUID created for events in Mule.</li>
 *  <li>log - A logger that can be used to write to Mule's log file.</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("groovy")
public @interface Groovy
{
    String value();

    boolean optional() default false;
    
}
