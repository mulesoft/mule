/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

public class PatchMethod extends EntityEnclosingMethod
{
    public PatchMethod()
    {
        super();
    }

    public PatchMethod(String uri) throws IllegalArgumentException, IllegalStateException
    {
        super(uri);
    }

    @Override
    public String getName()
    {
        return HttpConstants.METHOD_PATCH;
    }
}
