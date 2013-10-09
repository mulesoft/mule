/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

/**
 * An abstract implementation of a {@link MessageSequence}, that does not support
 * {@link #remove()}
 * 
 * @author flbulgarelli
 * @param <PayloadType>
 */
public abstract class AbstractMessageSequence<PayloadType> implements MessageSequence<PayloadType>
{
    public final boolean isEmpty()
    {
        return !hasNext();
    }

    public final void remove()
    {
        throw new UnsupportedOperationException();
    }
}
