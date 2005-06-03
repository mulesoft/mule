/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.container;

/**
 * <code>ContainerKeyPair</code> is a key strategy that binds a contrainer
 * reference with a container name. This object isn't used directly by users,
 * but it is used when the the Mule xml configuration is processed
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ContainerKeyPair
{
    private String contaimerName;
    private Object key;

    public ContainerKeyPair(String contaimerName, Object key)
    {
        this.contaimerName = contaimerName;
        this.key = key;
    }

    public String getContaimerName()
    {
        return contaimerName;
    }

    public Object getKey()
    {
        return key;
    }

    // here we only return the key value as string so that
    // containers that have no notion of this object can still
    // look up objects by calling the toString method on this object
    public String toString()
    {
        return key.toString();
    }
}
