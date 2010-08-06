/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.filters.EqualsFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

public class ChoiceRouterTestCase extends AbstractMuleTestCase
{
    private ChoiceRouter choiceRouter;

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
        choiceRouter = new ChoiceRouter();
    }

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

    public void testOnlyDefaultRoute() throws Exception
    {
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("foo:default", choiceRouter.process(getTestEvent("foo")).getMessageAsString());
    }

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

    public void testNoMatchingRouteWithDefaultRoute() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("foo:default", choiceRouter.process(getTestEvent("foo")).getMessageAsString());
    }

    public void testMatchingRouteWithDefaultRoute() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

    public void testMatchingRouteWithStatistics() throws Exception
    {
        choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.setRouterStatistics(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

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

    public void testUpdateRoute() throws Exception
    {
        TestMessageProcessor mp = new TestMessageProcessor("bar");
        choiceRouter.addRoute(mp, new EqualsFilter("paz"));
        choiceRouter.updateRoute(mp, new EqualsFilter("zap"));
        assertEquals("zap:bar", choiceRouter.process(getTestEvent("zap")).getMessageAsString());
    }

    public void testRemovingUpdatingMissingRoutes()
    {
        choiceRouter.updateRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
        choiceRouter.removeRoute(new TestMessageProcessor("rab"));
    }

}
