/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.multicast;

import org.mule.providers.udp.UdpMessageAdapter;
import org.mule.umo.MessagingException;
/**
 * <code>MulticastMessageAdapter</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MulticastMessageAdapter extends UdpMessageAdapter
{
    public MulticastMessageAdapter(Object message) throws MessagingException {
        super(message);
    }
}
