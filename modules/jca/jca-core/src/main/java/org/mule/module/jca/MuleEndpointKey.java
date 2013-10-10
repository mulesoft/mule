/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca;

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
