/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

/**
 * The EOFProtocol class is an application level tcp protocol that does nothing.
 * Reading is terminated by the stream being closed by the client.
 */
public class EOFProtocol extends DefaultProtocol
{

    /**
     * Repeat until end of file
     *
     * @param len Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    // @Override
    protected boolean isRepeat(int len, int available)
    {
        return true;
    }

}
