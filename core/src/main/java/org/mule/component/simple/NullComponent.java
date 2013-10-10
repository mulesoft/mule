/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
