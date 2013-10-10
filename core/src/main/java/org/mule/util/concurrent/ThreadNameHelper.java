/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import org.mule.api.MuleContext;
import org.mule.util.StringUtils;

/**
 * Encapsulates thread naming logic for Mule standalone and embedded scenarios.
 */
public class ThreadNameHelper
{

    private ThreadNameHelper()
    {
        // do not instantiate
    }

    public static String receiver(MuleContext muleContext, String connectorName)
    {
        return String.format("%s%s.receiver", getPrefix(muleContext), connectorName);
    }

    public static String dispatcher(MuleContext muleContext, String connectorName)
    {
        return String.format("%s%s.dispatcher", getPrefix(muleContext), connectorName);
    }

    public static String requester(MuleContext muleContext, String connectorName)
    {
        return String.format("%s%s.requester", getPrefix(muleContext), connectorName);
    }

    public static String async(MuleContext muleContext, String name, int sequenceNumber )
    {
        return String.format("%s%s.async%s", getPrefix(muleContext), name, sequenceNumber);
    }

    public static String sedaService(MuleContext muleContext, String name)
    {
        return String.format("%s%s", getPrefix(muleContext), name);
    }

    public static String flow(MuleContext muleContext, String name)
    {
        return String.format("%s%s", getPrefix(muleContext), name);
    }

    /**
     * Generate a generic thread name prefix for this context.
     * @param muleContext context to generate the name prefix for
     * @return "[appName]." if Mule is running as a container, otherwise empty string
     */
    public static String getPrefix(MuleContext muleContext)
    {
        final boolean containerMode = muleContext.getConfiguration().isContainerMode();
        final String id = muleContext.getConfiguration().getId();

        return containerMode ? String.format("[%s].", id) : StringUtils.EMPTY;
    }
}
