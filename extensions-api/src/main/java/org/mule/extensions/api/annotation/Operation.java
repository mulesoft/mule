/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Operation
{

    /**
     * The name of the element that will invoke this processor. This is optional and if it is not specified a name
     * will be derived from the name of the method.
     */
    String name() default "";

    /**
     * A user-friendly name for this processor.
     */
    String description() default "";

    Class<?>[] acceptedPayloadTypes() default Object.class;

}
