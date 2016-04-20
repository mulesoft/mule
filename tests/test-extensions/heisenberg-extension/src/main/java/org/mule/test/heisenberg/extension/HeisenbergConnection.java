/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

public class HeisenbergConnection
{

    private boolean connected = true;
    private final String saulPhoneNumber;

    public HeisenbergConnection(String saulPhoneNumber)
    {
        this.saulPhoneNumber = saulPhoneNumber;
    }

    public String callSaul()
    {
        return "You called " + saulPhoneNumber;
    }

    public void disconnect()
    {
        connected = false;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public String getSaulPhoneNumber()
    {
        return saulPhoneNumber;
    }
}
