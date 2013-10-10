/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.routing.AbstractMessageSequence;

public class ArrayMessageSequence extends AbstractMessageSequence<Object>
{

    private Object[] array;
    private int idx;

    public ArrayMessageSequence(Object[] array)
    {
        this.array = array;
        idx = 0;
    }

    @Override
    public int size()
    {
        return array.length - idx;
    }

    @Override
    public boolean hasNext()
    {
        return idx < array.length;
    }

    @Override
    public Object next()
    {
        return array[idx++];
    }

}


