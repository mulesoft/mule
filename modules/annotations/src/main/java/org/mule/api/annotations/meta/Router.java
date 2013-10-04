/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a router annotation and provides the type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE })
public @interface Router
{
    /**
     * The router type. {@link RouterType#Inbound} indicates that the router annotation is
     * used on the inbound flow on the service. {@link RouterType#Outbound} indicates that the
     * router will be used on the outbound flow. {@link RouterType#ReplyTo} indicates that the
     * router will be used for ReplyTo flows.
     * @return the type of Router
     */
    RouterType type();
}

