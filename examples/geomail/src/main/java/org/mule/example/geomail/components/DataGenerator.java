/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
            String address = new StringBuffer().append(generator.nextInt(255)).append(".")
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
