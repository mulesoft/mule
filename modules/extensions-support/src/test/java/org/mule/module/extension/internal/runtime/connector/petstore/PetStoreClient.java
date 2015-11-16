/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;

import java.util.List;

public class PetStoreClient
{

    private String username;
    private String password;
    private int disconnectCount;

    public PetStoreClient(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public List<String> getPets(String ownerName, PetStoreConnector config)
    {
        checkArgument(ownerName.equals(username), "config doesn't match");
        return config.getPets();
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public void disconnect()
    {
        disconnectCount++;
    }

    public int getDisconnectCount()
    {
        return disconnectCount;
    }

    public boolean isConnected()
    {
        checkState(disconnectCount >= 0, "negative disconnectCount");
        return disconnectCount == 0;
    }
}
