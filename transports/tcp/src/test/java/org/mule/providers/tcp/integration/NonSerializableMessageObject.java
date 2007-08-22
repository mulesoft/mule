/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

public class NonSerializableMessageObject
{
    public int i;
    public String s;
    public boolean b;

    public NonSerializableMessageObject(int i, String s, boolean b)
    {
        this.i = i;
        this.s = s;
        this.b = b;
    }
}
