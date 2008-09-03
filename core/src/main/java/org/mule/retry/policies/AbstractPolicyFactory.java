/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry.policies;

import org.mule.api.retry.PolicyFactory;

/**
 * Base class for PolicyFactory implementations
 * */
public abstract class AbstractPolicyFactory implements PolicyFactory
{
    /** should the retry template using this policy be executed in its own thread */
    protected boolean conectAsychronously;

    /**
     * should the retry template using this policy be executed in its own thread
     * @return
     */
    public boolean isConnectAsynchronously()
    {
        return conectAsychronously;
    }

    /**
     * should the retry template using this policy be executed in its own thread
     * @param conectAsychronously
     */
    public void setConectAsychronously(boolean conectAsychronously)
    {
        this.conectAsychronously = conectAsychronously;
    }
}
