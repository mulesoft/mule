/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

/**
 * TODO MULE-2233 Pooling functionality has been abstracted out of the SedaComponent. 
 * This class should be updated accordingly.
 */
public class SedaComponentStatistics extends ComponentStatistics
{
    private int componentPoolMaxSize = 0;
    private int componentPoolAbsoluteMaxSize = 0;
    private int componentPoolSize = 0;

    public SedaComponentStatistics(String name, int threadPoolSize, int componentPoolsize)
    {
        super(name, threadPoolSize);
        this.componentPoolMaxSize = componentPoolsize;
        this.componentPoolAbsoluteMaxSize = componentPoolMaxSize;
    }

    public synchronized void clear()
    {
        componentPoolSize = 0;
        componentPoolAbsoluteMaxSize = 0;
        super.clear();
    }

    public int getComponentPoolMaxSize()
    {
        return componentPoolMaxSize;
    }

    public int getComponentPoolAbsoluteMaxSize()
    {
        return componentPoolAbsoluteMaxSize;
    }

    public int getComponentPoolSize()
    {
        return componentPoolSize;
    }

    public synchronized void setComponentPoolSize(int componentPoolSize)
    {
        this.componentPoolSize = componentPoolSize;
        if (componentPoolSize > componentPoolAbsoluteMaxSize)
        {
            componentPoolAbsoluteMaxSize = componentPoolSize;
        }
    }
}
