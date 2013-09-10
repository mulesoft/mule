/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
