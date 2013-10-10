/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.component.simple.EchoService;

/**
 * <code>EchoComponent</code> will log the message and return the payload back as
 * the result.
 */
public class EchoComponent extends LogComponent implements EchoService
{

    @Override
    public Object onCall(MuleEventContext context) throws Exception
    {
        super.onCall(context);
        return context.getMessage();
    }

    public String echo(String echo)
    {
        return echo;
    }

}
