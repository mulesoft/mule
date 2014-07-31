/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

/**
 *  Class to check that system properties introduced to maintain backwards compatibility
 *  are enabled or not, the latter being the default. It can be used to track said properties.
 */
public class BackwardsCompatibilityPropertyChecker
{
    private final String propertyName;
    private Boolean override;

    public BackwardsCompatibilityPropertyChecker(String propertyName)
    {
        this.propertyName = propertyName;
    }

    public boolean isEnabled()
    {
        if (override == null)
        {
            return Boolean.getBoolean(propertyName);
        }
        else
        {
            return override;
        }
    }


    public void override(boolean override)
    {
        this.override = override;
    }

    public void removeOverride()
    {
        this.override = null;
    }
    public String getPropertyName()
    {
        return propertyName;
    }
}
