/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Default implementation of SocketPoolFactory
 * 
 * @since 2.2.6
 */
public class DefaultSocketPoolFactory implements SocketPoolFactory
{

    /**
     * {@inheritDoc}
     */
    public KeyedObjectPool createSocketPool(TcpConnector connector)
    {
        GenericKeyedObjectPool genericKeyedSocketsPool = new GenericKeyedObjectPool();
        genericKeyedSocketsPool.setTestOnBorrow(true);
        genericKeyedSocketsPool.setTestOnReturn(true);
        // There should only be one pooled instance per socket (key)
        genericKeyedSocketsPool.setMaxActive(1);
        genericKeyedSocketsPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);

        return genericKeyedSocketsPool;
    }

}
