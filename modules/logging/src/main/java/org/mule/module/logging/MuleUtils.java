/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

/**
 * Provides Mule related utilities without depending on Mule code
 */
public class MuleUtils
{

    public static final String MULE_HOME = "mule.home";

    private MuleUtils()
    {

    }

    /**
     * Indicates if Mule is running in standalone mode
     * @return
     */
    public static boolean isStandalone()
    {
        return System.getProperty(MULE_HOME) != null;
    }
}
