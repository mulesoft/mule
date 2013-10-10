/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

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

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof VoidResult;
    }

    @Override
    public int hashCode ()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{VoidResult}";
    }

}
