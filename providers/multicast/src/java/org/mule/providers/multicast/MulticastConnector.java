/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.multicast;

import org.mule.providers.udp.UdpConnector;

/**
 * <code>MulticastConnector</code> can dispatch mule events using ip
 * multicasting
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MulticastConnector extends UdpConnector
{
    private boolean loopback = false;

    public String getProtocol()
    {
        return "MULTICAST";
    }

    public boolean isLoopback()
    {
        return loopback;
    }

    public void setLoopback(boolean loopback)
    {
        this.loopback = loopback;
    }

}
