/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import java.io.ObjectStreamException;
import java.io.Serializable;

// @Immutable
public final class VoidResult implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3828573682818093673L;

    private static class VoidResultHolder
    {
        private static final VoidResult instance = new VoidResult();
    }

    public static VoidResult getInstance()
    {
        return VoidResultHolder.instance;
    }

    private VoidResult()
    {
        super();
    }

    private Object readResolve() throws ObjectStreamException
    {
        return VoidResultHolder.instance;
    }

    // @Override
    public boolean equals(Object obj)
    {
        return obj instanceof VoidResult;
    }

    // @Override
    public int hashCode ()
    {
        return 0;
    }

    // @Override
    public String toString()
    {
        return "{VoidResult}";
    }

}
