/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.cep;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class BrokerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "mule-config.xml";
    }

    @Test
    public void testBroker() throws Exception
    {
        Thread.sleep(20000); //don't set this too high without adjusting the test timeout first
        
//        MuleClient client = new MuleClient();
//
//        client.send("vm://stock.tick", new StockTick("SAP", 110, 0), null);
//        Thread.sleep(1000);
//        client.send("vm://stock.tick", new StockTick("SAP", 115, 2000), null);
//        Thread.sleep(1000);
//        client.send("vm://stock.tick", new StockTick("SAP", 100, 4000), null);
//        Thread.sleep(1000);
//        client.send("vm://stock.tick", new StockTick("SAP", 95, 6000), null);
//        Thread.sleep(1000);
//        client.send("vm://stock.tick", new StockTick("SAP", 80, 8000), null);
//        Thread.sleep(1000);
    }
/*
    0;RHT;$75.20
    0;JAVA;$59.90
    0;IBM;$112.11
    0;GOOG;$100.93
    0;YHOO;$90.22
    0;ORCL;$117.70
    0;MSFT;$105.19
    0;ORCL;$126.45
    1765;YHOO;$92.90
    3222;JAVA;$54.02
    4069;YHOO;$87.02
    5616;RHT;$82.53
    6070;YHOO;$84.40
    7500;MSFT;$113.51
    9297;MSFT;$107.77
    11123;YHOO;$78.65
    12632;IBM;$109.05
    13674;GOOG;$110.55
    14908;SAP;$113.46
    15328;MSFT;$102.97
    17268;RHT;$80.06
    17702;ORCL;$132.99
    19321;JAVA;$55.72
    20130;ORCL;$133.50
    21898;YHOO;$85.45
    23328;YHOO;$86.83
    24695;RHT;$82.39
    25263;SAP;$122.26
    25479;GOOG;$119.35
    26738;RHT;$77.92
    27268;YHOO;$86.59
    28720;MSFT;$110.58
    30697;MSFT;$106.29
    */
}


