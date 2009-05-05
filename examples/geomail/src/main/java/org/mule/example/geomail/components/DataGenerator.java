/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class DataGenerator implements Callable
{
    private Random generator = new Random();

    private int batchSize = 10;
    
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        eventContext.getMessage().setProperty("from.email.address", "testdatagenerator@geomail.com");

        //Create 3 for each run since many addresses will not be valid
        List ipAddresses = new ArrayList(batchSize);
        for (int i = 0; i < batchSize; i++)
        {
            String address = new StringBuffer().append(generator.nextInt(255)).append(".").append(generator.nextInt(255))
                    .append(".").append(generator.nextInt(255)).append(".").append(generator.nextInt(255)).toString();
              ipAddresses.add(address);

        }
        return ipAddresses;
    }

    public int getBatchSize()
    {
        return batchSize;
    }

    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }
}
