/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.api.MuleEvent;
import org.mule.exception.DefaultMessagingExceptionStrategy;

import org.apache.cxf.interceptor.Fault;

/**
 * This exception strategy forces the exception thrown from a web service invocation
 * to be passed as-is, not wrapped in a Mule exception object. This ensures the Cxf
 * serialiser/deserialiser can send the correct exception object to the client.
 *
 * @deprecated Currently the result is the same if no exception strategy is defined within the flow. The only difference
 * is that when you set the CxfComponentExceptionStrategy the exception is unwrapped inside of the exception block,
 * but the exceptionPayload doesn't change.
 */
@Deprecated
public class CxfComponentExceptionStrategy extends DefaultMessagingExceptionStrategy
{
    @Override
    protected void doHandleException(Exception e, MuleEvent event)
    {
        if (e.getCause() instanceof Fault)
        {
            super.doHandleException((Exception) e.getCause(), event);
        }
        else
        {
            super.doHandleException(e, event);
        }
    }
}
