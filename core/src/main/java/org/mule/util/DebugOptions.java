/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.config.MuleProperties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Mantains a static refernce to debug options in Mule. */
public final class DebugOptions
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DebugOptions.class);

    private static boolean cacheMessageAsBytes = getBooleanOption(MuleProperties.DEBUG_CACHE_MESSAGE_AS_BYTES, true);

    private static boolean cacheMessageOriginalPayload = getBooleanOption(MuleProperties.DEBUG_CACHE_ORIGINAL_MESSAGE, true);

    private static boolean enableStreaming = getBooleanOption(MuleProperties.DEBUG_ENABLE_STREAMING, true);

    private static boolean assertMessageAccess = getBooleanOption(MuleProperties.DEBUG_ASSERT_MESSAGE_ACCESS, true);

    private static boolean autoWrapMessageAwareTransform = getBooleanOption(MuleProperties.DEBUG_AUTO_WRAP_TRANSFORM, true);

    static
    {
        logDebugOptions();
    }

    private static boolean getBooleanOption(String name, boolean defaultValue)
    {
        String prop = System.getProperty(name, null);
        if(prop==null)
        {
            return defaultValue;
        }
        else
        {
            return Boolean.valueOf(prop).booleanValue();
        }
    }

    public static boolean isCacheMessageAsBytes()
    {
        return cacheMessageAsBytes;
    }

    public static void setCacheMessageAsBytes(boolean cacheMessageAsBytes)
    {
        if(isCacheMessageAsBytes()!=cacheMessageAsBytes)
        {
            assertChange(MuleProperties.DEBUG_CACHE_MESSAGE_AS_BYTES, String.valueOf(cacheMessageAsBytes));
        }
        DebugOptions.cacheMessageAsBytes = cacheMessageAsBytes;
    }

    public static boolean isCacheMessageOriginalPayload()
    {
        return cacheMessageOriginalPayload;
    }

    public static void setCacheMessageOriginalPayload(boolean cacheMessageOriginalPayload)
    {
        if(isCacheMessageOriginalPayload()!=cacheMessageOriginalPayload)
        {
            assertChange(MuleProperties.DEBUG_CACHE_ORIGINAL_MESSAGE, String.valueOf(cacheMessageOriginalPayload));
        }
        DebugOptions.cacheMessageOriginalPayload = cacheMessageOriginalPayload;
    }

    public static boolean isEnableStreaming()
    {
        return enableStreaming;
    }

    public static void setEnableStreaming(boolean enableStreaming)
    {
        if(isEnableStreaming()!=enableStreaming)
        {
            assertChange(MuleProperties.DEBUG_ENABLE_STREAMING, String.valueOf(enableStreaming));
        }
        DebugOptions.enableStreaming = enableStreaming;
    }

    public static boolean isAssertMessageAccess()
    {
        return assertMessageAccess;
    }

    public static void setAssertMessageAccess(boolean assertMessageAccess)
    {
        if(isAssertMessageAccess()!=assertMessageAccess)
        {
            assertChange(MuleProperties.DEBUG_ASSERT_MESSAGE_ACCESS, String.valueOf(assertMessageAccess));
        }
        DebugOptions.assertMessageAccess = assertMessageAccess;
    }

    protected static void assertChange(String property, String value)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(StringMessageUtils.getBoilerPlate("Debug property changed is now: " + property + "=" + value));
        }
    }

    public static boolean isAutoWrapMessageAwareTransform()
    {
        return autoWrapMessageAwareTransform;
    }

    public static void setAutoWrapMessageAwareTransform(boolean autoWrapMessageAwareTransform)
    {
        if(isAutoWrapMessageAwareTransform()!=autoWrapMessageAwareTransform)
        {
            assertChange(MuleProperties.DEBUG_AUTO_WRAP_TRANSFORM, String.valueOf(autoWrapMessageAwareTransform));
        }
        DebugOptions.autoWrapMessageAwareTransform = autoWrapMessageAwareTransform;
    }

    public static void logDebugOptions()
    {
        if(logger.isDebugEnabled())
        {
            List values = new ArrayList(5);
            values.add("Mule Debug Options:");
            values.add(MuleProperties.DEBUG_CACHE_MESSAGE_AS_BYTES + "=" + isCacheMessageAsBytes());
            values.add(MuleProperties.DEBUG_CACHE_ORIGINAL_MESSAGE + "=" + isCacheMessageOriginalPayload());
            values.add(MuleProperties.DEBUG_ASSERT_MESSAGE_ACCESS + "=" + isAssertMessageAccess());
            values.add(MuleProperties.DEBUG_ENABLE_STREAMING + "=" + isEnableStreaming());
            values.add(MuleProperties.DEBUG_AUTO_WRAP_TRANSFORM + "=" + isAutoWrapMessageAwareTransform());
            logger.debug(StringMessageUtils.getBoilerPlate(values, '*', 80));
        }

    }
}
