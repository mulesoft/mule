/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.apache.commons.pool.KeyedObjectPool;

/**
 * A factory for socket pools
 *
 * @since 2.2.6
 */
public interface SocketPoolFactory
{

    /**
     * Creates a keyed socket pool using the information in the TCP connector
     * 
     * @param connector the TCP connector used for configuration
     * @return the new keyed socket pool
     */
    KeyedObjectPool createSocketPool(TcpConnector connector);
}
