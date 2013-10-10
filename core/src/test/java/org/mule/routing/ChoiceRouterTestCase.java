/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.filters.EqualsFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChoiceRouterTestCase extends AbstractMuleContextTestCase
{
    private ChoiceRouter choiceRouter;

    public ChoiceRouterTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        choiceRouter = new ChoiceRouter();
    }

    @Test
    public void testNoRoute() throws Exception
    {
        try
        {
            choiceRouter.process(getTestEvent("foo"));
            fail("should have got a MuleException");
        }
        catch (MuleException me)
        {
            assertTrue(me instanceof RoutePathNotFoundException);
        }
    }

    @Test
    public void testOnlyDefaultRoute() throws Exception
    {
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("foo:default", choiceRouter.process(getTestEvent("foo")).getMessageAsString());
    }

    @Test
    public void testNoMatchingNorDefaultRoute() throws Exception
    {
        try
        {
            choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
            choiceRouter.process(getTestEvent("foo"));
            fail("should have got a MuleException");
        }
        catch (MuleException me)
        {
            assertTrue(me instanceof RoutePathNotFoundException);
        }
    }

    @Test
    public void testNoMatchingRouteWithDefaultRoute() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("foo:default", choiceRouter.process(getTestEvent("foo")).getMessageAsString());
    }

    @Test
    public void testMatchingRouteWithDefaultRoute() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

    @Test
    public void testMatchingRouteWithStatistics() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setRouterStatistics(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

    @Test
    public void testAddAndDeleteRoute() throws Exception
    {
        try
        {
            TestMessageProcessor mp = new TestMessageProcessor("bar");
            choiceRouter.addRoute(mp, new EqualsFilter("zap"));
            choiceRouter.removeRoute(mp);
            choiceRouter.setRouterStatistics(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
            choiceRouter.process(getTestEvent("zap"));
            fail("should have got a MuleException");
        }
        catch (MuleException me)
        {
            assertTrue(me instanceof RoutePathNotFoundException);
        }
    }

    @Test
    public void testUpdateRoute() throws Exception
    {
        TestMessageProcessor mp = new TestMessageProcessor("bar");
        choiceRouter.addRoute(mp, new EqualsFilter("paz"));
        choiceRouter.updateRoute(mp, new EqualsFilter("zap"));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

    @Test
    public void testRemovingUpdatingMissingRoutes()
    {
        choiceRouter.updateRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.removeRoute(new TestMessageProcessor("rab"));
    }

}
