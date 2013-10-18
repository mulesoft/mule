/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleEvent;
import org.mule.routing.MessageFilter;

public class ExceptionThrowingInboundRouter extends MessageFilter
{
    /**
     * For IoC only
     */
    @Deprecated
    public ExceptionThrowingInboundRouter()
    {
        super();
    }

    @Override
    public boolean accept(MuleEvent event)
    {
        throw new RuntimeException();
    }
}
