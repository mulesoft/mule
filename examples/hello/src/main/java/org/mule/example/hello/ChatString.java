/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
