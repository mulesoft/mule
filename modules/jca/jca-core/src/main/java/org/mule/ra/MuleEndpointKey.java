/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import javax.resource.spi.endpoint.MessageEndpointFactory;

/**
 * <code>MuleEndpointKey</code> TODO
 */
public class MuleEndpointKey
{
    final private MessageEndpointFactory messageEndpointFactory;
    final private MuleActivationSpec activationSpec;

    /**
     * @param messageEndpointFactory
     * @param activationSpec
     */
    public MuleEndpointKey(MessageEndpointFactory messageEndpointFactory, MuleActivationSpec activationSpec)
    {
        this.messageEndpointFactory = messageEndpointFactory;
        this.activationSpec = activationSpec;
    }

    /**
     * @return Returns the activationSpec.
     */
    public MuleActivationSpec getActivationSpec()
    {
        return activationSpec;
    }

    /**
     * @return Returns the messageEndpointFactory.
     */
    public MessageEndpointFactory getMessageEndpointFactory()
    {
        return messageEndpointFactory;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return messageEndpointFactory.hashCode() ^ activationSpec.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (this.getClass() != obj.getClass())
        {
            return false;
        }

        MuleEndpointKey o = (MuleEndpointKey)obj;
        return o.activationSpec == activationSpec && o.messageEndpointFactory == messageEndpointFactory;
    }

    public String toString()
    {
        return "MuleEndpointKey{" + "messageEndpointFactory=" + messageEndpointFactory + ", activationSpec="
               + activationSpec + "}";
    }
}
