/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
