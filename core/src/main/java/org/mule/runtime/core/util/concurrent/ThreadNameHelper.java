/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
