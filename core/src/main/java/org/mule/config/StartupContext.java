/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import java.util.Collections;
import java.util.Map;

/**
 * A class holding cross-cutting startup info.
 */
public class StartupContext
{

    private static final ThreadLocal<StartupContext> info = new ThreadLocal<StartupContext>(){
        @Override
        protected StartupContext initialValue()
        {
            return new StartupContext();
        }
    };

    private Map startupOptions = Collections.EMPTY_MAP;

    public static StartupContext get()
    {
        return info.get();
    }

    public Map getStartupOptions()
    {
        return Collections.unmodifiableMap(startupOptions);
    }

    public void setStartupOptions(Map startupOptions)
    {
        this.startupOptions = startupOptions;
    }
}
