/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * <code>NullPayload</code> represents a null event payload
 */
// @Immutable
public final class NullPayload implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3530905899811505080L;

    private static class NullPayloadHolder
    {
        private static final NullPayload instance = new NullPayload();
    }

    public static NullPayload getInstance()
    {
        return NullPayloadHolder.instance;
    }

    private NullPayload()
    {
        super();
    }

    private Object readResolve() throws ObjectStreamException
    {
        return NullPayloadHolder.instance;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof NullPayload;
    }

    @Override
    public int hashCode ()
    {
        return 1; // random, 0 is taken by VoidResult
    }

    @Override
    public String toString()
    {
        return "{NullPayload}";
    }

}
