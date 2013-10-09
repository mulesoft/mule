/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
