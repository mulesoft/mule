/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

public class LiteralMessage implements Message
{

    private byte[] raw;

    public LiteralMessage(byte[] raw)
    {
        this.raw = raw;
    }

    public String getTextData()
    {
        return new String(this.raw);
    }

}
