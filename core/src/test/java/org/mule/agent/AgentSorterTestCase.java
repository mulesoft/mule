/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AgentSorterTestCase extends TestCase
{
    public void testListWithoutDependencies()
    {
        MockAgent a = new MockAgent_A();
        MockAgent b = new MockAgent_B();
        MockAgent c = new MockAgent_C();

        List agents = new ArrayList();
        agents.add(a);
        agents.add(b);
        agents.add(c);

        List result = AgentSorter.sortAgents(agents);
        Assert.assertEquals(3, result.size());
    }
    
    public void testSortWithSimpleDependency()
    {
        MockAgent a = new MockAgent_A();
        MockAgent b = new MockAgent_B();
        MockAgent c = new MockAgent_C(new Class[] { MockAgent_A.class });
        
        List agents = new ArrayList();
        agents.add(a);
        agents.add(b);
        agents.add(c);

        List result = AgentSorter.sortAgents(agents);
        Assert.assertEquals(3, result.size());
        
        int indexOfA = result.indexOf(a);
        int indexOfC = result.indexOf(c);
        Assert.assertTrue(indexOfA < indexOfC);
    }

    public void testSortWithForwardDependency()
    {
        MockAgent a = new MockAgent_A(new Class[] { MockAgent_C.class });
        MockAgent b = new MockAgent_B();
        MockAgent c = new MockAgent_C();
        
        List agents = new ArrayList();
        agents.add(a);
        agents.add(b);
        agents.add(c);

        List result = AgentSorter.sortAgents(agents);
        Assert.assertEquals(3, result.size());
        
        int indexOfA = result.indexOf(a);
        Assert.assertTrue(indexOfA > -1);
        int indexOfC = result.indexOf(c);
        Assert.assertTrue(indexOfC > -1);
        Assert.assertTrue(indexOfC < indexOfA);
    }
        
    public void testCyclicDependency()
    {
        MockAgent a = new MockAgent_A(new Class[] { MockAgent_B.class });
        MockAgent b = new MockAgent_B(new Class[] { MockAgent_A.class });
        
        List agents = new ArrayList();
        agents.add(a);
        agents.add(b);

        try
        {
            AgentSorter.sortAgents(agents);
            Assert.fail();
        }
        catch (IllegalArgumentException iae)
        {
            // expected exception
        }
    }
    
    public void testSortComplexDependencies()
    {
        MockAgent a = new MockAgent_A();
        MockAgent b = new MockAgent_B(new Class[] { MockAgent_A.class });
        MockAgent c = new MockAgent_C(new Class[] { MockAgent_B.class });
        MockAgent d = new MockAgent_D(new Class[] { MockAgent_A.class, MockAgent_C.class });
        
        List agents = new ArrayList();
        agents.add(a);
        agents.add(d);
        agents.add(c);
        agents.add(b);

        List result = AgentSorter.sortAgents(agents);
        Assert.assertEquals(4, result.size());
        
        Assert.assertEquals(a, result.get(0));
        Assert.assertEquals(b, result.get(1));
        Assert.assertEquals(c, result.get(2));
        Assert.assertEquals(d, result.get(3));
    }
    
    public void testSortWithMissingDependency()
    {
        MockAgent a = new MockAgent_A();
        MockAgent b = new MockAgent_B(new Class[] { MockAgent_C.class });
        
        List agents = new ArrayList();
        agents.add(a);
        agents.add(b);

        List result = AgentSorter.sortAgents(agents);
        Assert.assertEquals(2, result.size());
    }

}


