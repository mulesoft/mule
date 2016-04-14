/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * <code>NullComponent</code> is a service that is used as a placeholder. This
 * implementation will throw an exception if a message is received for it.
 */
public class NullComponent implements Callable
{

    public Object onCall(MuleEventContext context) throws Exception
    {
        throw new UnsupportedOperationException("This service cannot receive messages. Service is: "
                                                + context.getFlowConstruct().getName());
    }

}
