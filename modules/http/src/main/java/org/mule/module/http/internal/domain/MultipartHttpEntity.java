/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.domain;

import java.util.Collection;

import javax.servlet.http.Part;

public class MultipartHttpEntity implements HttpEntity
{

    private final Collection<Part> parts;

    public MultipartHttpEntity(final Collection<Part> parts)
    {
        this.parts = parts;
    }

    public Collection<Part> getParts()
    {
        return this.parts;
    }


}
