/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import java.io.Serializable;

/**
 * Simple Custom Serializable object to check that Custom Objects Can Actually be
 * Chunked
 */
class SimpleSerializableObject implements Serializable
{
    private static final long serialVersionUID = 4705305160224612898L;
    public String s;
    public boolean b;
    public int i;

    public SimpleSerializableObject(String s, boolean b, int i)
    {
        this.s = s;
        this.b = b;
        this.i = i;
    }
    
}
