/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

public abstract class AbstractResponseMessageProcessor extends AbstractInterceptingMessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return processResponse(processNext(event));
    }

    protected abstract MuleEvent processResponse(MuleEvent processNext) throws MuleException;

}
