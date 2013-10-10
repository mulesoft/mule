/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing;

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
