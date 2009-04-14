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
