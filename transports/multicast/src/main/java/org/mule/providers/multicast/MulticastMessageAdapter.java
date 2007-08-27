/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.providers.udp.UdpMessageAdapter;
import org.mule.umo.MessagingException;

/**
 * <code>MulticastMessageAdapter</code> TODO
 */

public class MulticastMessageAdapter extends UdpMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4515594269344311534L;

    public MulticastMessageAdapter(Object message) throws MessagingException
    {
        super(message);
    }
}
