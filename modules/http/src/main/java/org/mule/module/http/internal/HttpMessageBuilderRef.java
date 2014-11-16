/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

/**
 * Object that holds a reference to an {@code HttpMessageBuilder}, it represents the {@code <http:builder ref="..." />}
 * element for composing request/response builders.
 */
public class HttpMessageBuilderRef
{
    private HttpMessageBuilder ref;


    public HttpMessageBuilder getRef()
    {
        return ref;
    }

    public void setRef(HttpMessageBuilder ref)
    {
        this.ref = ref;
    }
}
