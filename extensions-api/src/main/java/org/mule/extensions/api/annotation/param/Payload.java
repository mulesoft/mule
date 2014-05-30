/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.api.annotation.param;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on components, this annotation marks the method parameter that will be used
 * to pass in the message payload. Note that the parameter type will be used to do
 * any auto conversions using transformers available inside the Mule container. Mule
 * has a number of standard transformers for dealing with common Java types such as
 * XML documents, streams, byte arrays, strings, etc.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload
{

}
