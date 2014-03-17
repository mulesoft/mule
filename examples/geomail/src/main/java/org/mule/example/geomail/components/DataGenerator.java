/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TODO
 */
public class DataGenerator implements Callable
{
    private Random generator = new Random();
    private int batchSize = 10;
    
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        eventContext.getMessage().setOutboundProperty("from.email.address", "testdatagenerator@geomail.com");

        // Create multiple IPs for each run since many addresses will not be valid
        List<String> ipAddresses = new ArrayList<String>(batchSize);
        for (int i = 0; i < batchSize; i++)
        {
            String address = new StringBuilder().append(generator.nextInt(255)).append(".")
                .append(generator.nextInt(255)).append(".").append(generator.nextInt(255)).
                append(".").append(generator.nextInt(255)).toString();
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
