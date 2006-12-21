/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.tcp;

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
