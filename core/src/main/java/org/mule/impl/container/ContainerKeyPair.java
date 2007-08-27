/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

/**
 * <code>ContainerKeyPair</code> is a key strategy that binds a container reference
 * with a container name. This object isn't used directly by users, but it is used
 * when the the Mule XML configuration is processed.
 */
public class ContainerKeyPair
{
    private final String containerName;
    private final Object key;
    private final boolean required;

    public ContainerKeyPair(String containerName, Object key)
    {
        this.containerName = containerName;
        this.key = key;
        this.required = true;
    }

    public ContainerKeyPair(String containerName, Object key, boolean required)
    {
        this.containerName = containerName;
        this.key = key;
        this.required = required;
    }

    public String getContainerName()
    {
        return containerName;
    }

    public Object getKey()
    {
        return key;
    }

    public boolean isRequired()
    {
        return required;
    }

    // here we only return the key value as string so that
    // containers that have no notion of this object can still
    // look up objects by calling the toString method on this object
    public String toString()
    {
        return key.toString();
    }

    public String toFullString()
    {
        return "Container Key{key=" + key.toString() + ", container=" + containerName + ", required="
               + required + "}";
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final ContainerKeyPair that = (ContainerKeyPair) o;

        if(containerName!=null)
        {
            if (!containerName.equals(that.containerName))
            {
                return false;
            }
        }
        if (!key.equals(that.key))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return 29 * (containerName == null ? 0 : containerName.hashCode()) + key.hashCode();
    }
}
