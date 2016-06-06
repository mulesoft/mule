/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import java.io.Serializable;

public class MessageObject implements Serializable
{
    private static final long serialVersionUID = 6623028039115997808L;
    public int i;
    public String s;
    public boolean b;

    public MessageObject(int i, String s, boolean b)
    {
        this.i = i;
        this.s = s;
        this.b = b;
    }
}
