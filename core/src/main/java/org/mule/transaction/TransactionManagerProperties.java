package org.mule.transaction;
/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


/** A singleton that holds the configured properties for the JTA transaction manager */
public class TransactionManagerProperties
{
    /** Whether to join "external" transactions, i.e. those created outside Mule */
    private boolean joinExternal;

    public TransactionManagerProperties()
    {
        joinExternal = false;
    }

    public boolean isJoinExternal()
    {
        return joinExternal;
    }

    public void setJoinExternal(boolean joinExternal)
    {
        this.joinExternal = joinExternal;
    }
}
