/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
