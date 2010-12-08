/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
        return String.format("%s%s.receiver", getAppPrefix(muleContext), connectorName);
    }

    public static String dispatcher(MuleContext muleContext, String connectorName)
    {
        return String.format("%s%s.dispatcher", getAppPrefix(muleContext), connectorName);
    }

    public static String requester(MuleContext muleContext, String connectorName)
    {
        return String.format("%s%s.requester", getAppPrefix(muleContext), connectorName);
    }

    public static String asyncProcessor(MuleContext muleContext, String mpName)
    {
        return String.format("%s%s.processor.async", getAppPrefix(muleContext), mpName);
    }

    public static String sedaService(MuleContext muleContext, String name)
    {
        return String.format("%sseda.%s", getAppPrefix(muleContext), name);
    }

    public static String flow(MuleContext muleContext, String name)
    {
        return String.format("%sflow.%s", getAppPrefix(muleContext), name);

    }

    /**
     * Generate a generic thread name prefix for this application.
     * @param muleContext
     * @return "[appName]." if Mule is running as a container, otherwise empty string
     */
    public static String getAppPrefix(MuleContext muleContext)
    {
        final boolean containerMode = muleContext.getConfiguration().isContainerMode();
        final String id = muleContext.getConfiguration().getId();

        return containerMode ? String.format("[%s].", id) : StringUtils.EMPTY;
    }
}
