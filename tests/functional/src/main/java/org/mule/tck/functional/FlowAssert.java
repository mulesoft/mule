/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FlowAssert
{

    private static Map<String, List<AssertionMessageProcessor>> assertions = new TreeMap<String, List<AssertionMessageProcessor>>();

    public static void verify() throws Exception
    {
        for (List<AssertionMessageProcessor> flowAssertions : assertions.values())
        {
            for (AssertionMessageProcessor assertion : flowAssertions)
            {
                assertion.verify();
            }
        }
    }

    public static void verify(String flowName) throws Exception
    {

        List<AssertionMessageProcessor> flowAssertions = assertions.get(flowName);
        if (flowAssertions != null)
        {
            for (AssertionMessageProcessor assertion : flowAssertions)
            {
                assertion.verify();
            }
        }
    }

    static void addAssertion(String flowName, AssertionMessageProcessor assertion)
    {
        if (assertions.get(flowName) == null)
        {
            assertions.put(flowName, new ArrayList<AssertionMessageProcessor>());
        }
        assertions.get(flowName).add(assertion);
    }

    public static void reset()
    {
        assertions = new TreeMap<String, List<AssertionMessageProcessor>>();
    }

}
