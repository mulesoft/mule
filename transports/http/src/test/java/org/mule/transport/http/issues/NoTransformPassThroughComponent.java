/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.issues;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

public class NoTransformPassThroughComponent implements Callable
{

    public Object onCall(MuleEventContext context) throws Exception
    {
        return context.getMessage();
    }

}
