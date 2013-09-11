/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.hello;

import java.io.Serializable;

/**
 * <code>ChatString</code> TODO (document class)
 */
public class ChatString implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3140370545357738491L;

    private StringBuffer string = new StringBuffer();

    /**
     * @param str
     */
    public StringBuffer append(String str)
    {
        return string.append(str);
    }

    /**
     * @param sb
     */
    public StringBuffer append(StringBuffer sb)
    {
        return string.append(sb);
    }

    /**
     * @param offset
     * @param str
     */
    public StringBuffer insert(int offset, char[] str)
    {
        return string.insert(offset, str);
    }

    /**
     * @param index
     * @param str
     * @param offset
     * @param len
     */
    public StringBuffer insert(int index, char[] str, int offset, int len)
    {
        return string.insert(index, str, offset, len);
    }

    @Override
    public String toString()
    {
        return string.toString();
    }

    public int getSize()
    {
        return string.length();
    }

}
