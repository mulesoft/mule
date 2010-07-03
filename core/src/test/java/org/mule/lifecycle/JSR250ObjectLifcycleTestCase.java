/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.tck.AbstractMuleTestCase;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Test lifecycle behaviour and restrictions on lifecyce methods
 */
public class JSR250ObjectLifcycleTestCase extends AbstractMuleTestCase
{
    public void testNormalBehaviour() throws Exception
    {
        JSR250ObjectLifecycleTracker tracker = new JSR250ObjectLifecycleTracker();
        muleContext.getRegistry().registerObject("test", tracker);

        muleContext.dispose();
        assertEquals("[setMuleContext, initialise, dispose]", tracker.getTracker().toString());
    }

    public void testTwoPostConstructAnnotations() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("test", new DupePostConstructJSR250ObjectLifecycleTracker());
            fail("Object has two @PostConstruct annotations");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    public void testTwoPreDestroyAnnotations() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("test", new DupePreDestroyJSR250ObjectLifecycleTracker());
            fail("Object has two @PreDestroy annotations");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    public void testBadPostConstructMethod() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("test", new BadPostConstructLifecycleMethodObject());
            fail("PostContruct Lifecycle method has a non-void return type");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    public void testBadPreDestroyMethod() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("test", new BadPreDestroyLifecycleMethodObject());
            fail("PreDestroy Lifecycle method has a parameter");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    public class DupePostConstructJSR250ObjectLifecycleTracker extends JSR250ObjectLifecycleTracker
    {
        //You cannot have an object with two {@link PostConstruct} annotated methods
        @PostConstruct
        public void init2()
        {
            getTracker().add("initialise 2");
        }
    }

    public class DupePreDestroyJSR250ObjectLifecycleTracker extends JSR250ObjectLifecycleTracker
    {
        //You cannot have an object with two {@link PostConstruct} annotated methods
        @PreDestroy
        public void dispose2()
        {
            getTracker().add("dispose 2");
        }
    }

    public class BadPostConstructLifecycleMethodObject
    {
        @PostConstruct
        public boolean init()
        {
            return true;
        }
    }

    public class BadPreDestroyLifecycleMethodObject
    {
        @PreDestroy
        public void destroy(boolean foo)
        {

        }
    }
}
