/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import java.util.Collections;
import java.util.Map;

/**
 * A class holding cross-cutting startup info.
 */
public class StartupContext
{
    private static final ThreadLocal<StartupContext> info = new ThreadLocal<StartupContext>()
    {
        @Override
        protected StartupContext initialValue()
        {
            return new StartupContext();
        }
    };

    private Map<String, Object> startupOptions = Collections.emptyMap();

    public static StartupContext get()
    {
        return info.get();
    }

    public Map<String, Object> getStartupOptions()
    {
        return Collections.unmodifiableMap(startupOptions);
    }

    public void setStartupOptions(Map<String, Object> startupOptions)
    {
        this.startupOptions = startupOptions;
    }
}
