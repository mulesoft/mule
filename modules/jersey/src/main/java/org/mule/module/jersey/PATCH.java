/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Target({ElementType.METHOD})
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@javax.ws.rs.HttpMethod("PATCH")
public @interface PATCH
{
    // support for the HTTP PATCH method
}
