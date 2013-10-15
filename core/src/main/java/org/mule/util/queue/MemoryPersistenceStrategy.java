/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.util.UUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MemoryPersistenceStrategy implements QueuePersistenceStrategy
{
    private Map map = Collections.synchronizedMap(new HashMap());

    protected Object getId(Object obj)
    {
        return UUID.getUUID();
    }

    public Object store(String queue, Object obj) throws IOException
    {
        if (obj == null)
        {
            throw new IllegalArgumentException("Cannot store null object.");
        }
        Object id = getId(obj);
        map.put(id, obj);
        return id;
    }

    public Object load(String queue, Object id) throws IOException
    {
        return map.get(id);
    }

    public void remove(String queue, Object id) throws IOException
    {
        map.remove(id);
    }

    public List restore() throws IOException
    {
        return new ArrayList();
    }

    public void open() throws IOException
    {
        // nothing to do
    }

    public void close() throws IOException
    {
        // nothing to do
    }

    public boolean isTransient()
    {
        return true;
    }
}
