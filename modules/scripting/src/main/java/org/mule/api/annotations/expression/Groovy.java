/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
