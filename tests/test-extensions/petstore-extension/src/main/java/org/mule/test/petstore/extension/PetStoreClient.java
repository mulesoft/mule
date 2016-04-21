/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;


import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ThreadingProfile;

import java.util.List;

public class PetStoreClient
{

    private String username;
    private String password;
    private TlsContextFactory tlsContext;

    private ThreadingProfile threadingProfile;

    private int disconnectCount;
    public PetStoreClient(String username, String password, TlsContextFactory tlsContextFactory, ThreadingProfile threadingProfile)
    {
        this.username = username;
        this.password = password;
        this.tlsContext = tlsContextFactory;
        this.threadingProfile = threadingProfile;
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

    public TlsContextFactory getTlsContext()
    {
        return tlsContext;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }
}
