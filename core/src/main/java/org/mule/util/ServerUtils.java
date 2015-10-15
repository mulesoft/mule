/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.mule.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

/**
 * Server instance utility methods.
 *
 * This utility methods are not meant to be used in container mode.
 */
public class ServerUtils
{

    /**
     * @return the MULE_HOME directory of this instance. Returns null if the property is not set
     */
    public static String getMuleHome()
    {
        return System.getProperty(MULE_HOME_DIRECTORY_PROPERTY);
    }

    /**
     * @return the MULE_BASE directory of this instance. Returns the mule.home property value if mule.base is not set which may be null.
     */
    public static String getMuleBase()
    {
        String muleBaseDir = System.getProperty(MULE_BASE_DIRECTORY_PROPERTY);
        return muleBaseDir != null ? muleBaseDir : getMuleHome();
    }

}
