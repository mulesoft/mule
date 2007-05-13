/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

public class NamedPayload
{

    public static final String DEFAULT_NAME = "default-file-name";
    private byte[] payload;
    private String name;

    public NamedPayload(byte[] payload)
    {
        this(DEFAULT_NAME, payload);
    }

    public NamedPayload(String name, byte[] payload)
    {
        setName(name);
        setPayload(payload);
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    // @Override
    public String toString()
    {
        try
        {
            return name + " " + new String(payload);
        }
        catch(Exception e)
        {
            return name + " " + payload;
        }
    }

}
