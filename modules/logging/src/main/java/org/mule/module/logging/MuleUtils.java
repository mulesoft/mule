/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
