/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights the issue: MULE-4599 where dispose cannot be called on a transformer since it is a prototype in Spring, so spring
 * does not manage the object.
 */
public class TransformerLifecycleTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "simple-transformer-config.xml";
    }

    public void testLifecycleInSpring() throws Exception
    {
        TransformerLifecycleTracker transformer = (TransformerLifecycleTracker)muleContext.getRegistry().lookupTransformer("lifecycle");
        assertNotNull(transformer);
        muleContext.dispose();
        assertLifecycle(transformer);
    }


    public void testLifecycleInTransientRegistry() throws Exception
    {
        TransformerLifecycleTracker transformer = new TransformerLifecycleTracker();
        transformer.setProperty("foo");
        muleContext.getRegistry().registerTransformer(transformer);
        muleContext.dispose();
        assertLifecycle(transformer);
    }

    private void assertLifecycle(TransformerLifecycleTracker transformer)
    {
        assertEquals("[setProperty, initialise, dispose]", transformer.getTracker().toString());
    }



    public static class TransformerLifecycleTracker extends AbstractTransformer implements Disposable
    {
        private final List<String> tracker = new ArrayList<String>();

        private String property;

        @Override
        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            tracker.add("doTransform");
            return null;
        }

        public String getProperty()
        {
            return null;
        }

        public void setProperty(String property)
        {
            tracker.add("setProperty");
        }

        public List<String> getTracker()
        {
            return tracker;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            tracker.add("initialise");
        }

        public void dispose()
        {
            tracker.add("dispose");
        }
    }
}
