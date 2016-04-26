/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.construct;

import java.util.HashSet;
import java.util.Set;

public class MuleConnectionsBuilder
{

    public class MuleConnection
    {
        private String protocol;
        private String address;
        private MuleConnectionDirection direction;
        private boolean connected;
        private String description;

        @Override
        public String toString()
        {
            return protocol + "://" + address + " (" + direction + ", " + connected + ") " + description;
        }

        public String getProtocol()
        {
            return protocol;
        }

        public String getAddress()
        {
            return address;
        }

        public MuleConnectionDirection getDirection()
        {
            return direction;
        }

        public boolean isConnected()
        {
            return connected;
        }

        public String getDescription()
        {
            return description;
        }
    }


    public enum MuleConnectionDirection
    {
        TO, FROM;
    }

    private MuleConnection provided;
    private Set<MuleConnection> consumed = new HashSet<>();


    public void setProvided(String protocol, String address, MuleConnectionDirection direction, boolean connected, String description)
    {
        final MuleConnection mc = buildConnection(protocol, address, direction, connected, description);
        provided = mc;
    }

    public void addConsumed(String protocol, String address, MuleConnectionDirection direction, boolean connected, String description)
    {
        final MuleConnection mc = buildConnection(protocol, address, direction, connected, description);
        consumed.add(mc);
    }

    protected MuleConnection buildConnection(String protocol, String address, MuleConnectionDirection direction, boolean connected, String description)
    {
        final MuleConnection mc = new MuleConnection();
        mc.protocol = protocol.toUpperCase();

        if (address.contains("://"))
        {
            mc.address = address.substring(address.indexOf("://") + 3);
        }
        else
        {
            mc.address = address;
        }

        mc.direction = direction;
        mc.connected = connected;
        mc.description = description;
        return mc;
    }

    @Override
    public String toString()
    {
        return "MuleConnectionsBuilder[provided: " + (provided != null ? provided.toString() : "null") + "; consumed: " + consumed.toString() + "]";
    }

    public MuleConnection getProvidedConnection()
    {
        return provided;
    }

    public Set<MuleConnection> getConsumedConnections()
    {
        return consumed;
    }
}
