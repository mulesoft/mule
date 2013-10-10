/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.stats;

/**
 * TODO MULE-2233 Pooling functionality has been abstracted out of the SedaService. 
 * This class should be updated accordingly.
 */
public class SedaServiceStatistics extends ServiceStatistics
{
    private int componentPoolMaxSize = 0;
    private int componentPoolAbsoluteMaxSize = 0;
    private int componentPoolSize = 0;

    public SedaServiceStatistics(String name, int threadPoolSize, int componentPoolsize)
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
