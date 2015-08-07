/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Highlights the issue: MULE-4599 where dispose cannot be called on a transformer
 * since it is a prototype in Spring, so spring does not manage the object.
 */
public class RegistryTransformerLifecycleTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "simple-transformer-config.xml";
    }

    @Test
    public void testLifecycleInSpring() throws Exception
    {
        TransformerLifecycleTracker transformer = (TransformerLifecycleTracker) muleContext.getRegistry()
            .lookupTransformer("lifecycle");
        assertNotNull(transformer);
        muleContext.dispose();
        assertInitialise(transformer);
    }

    @Test
    public void testLifecycleInFlowInSpring() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("flow");
        TransformerLifecycleTracker transformer = (TransformerLifecycleTracker) flow.getMessageProcessors()
            .get(0);
        assertNotNull(transformer);

        muleContext.dispose();
        assertLifecycle(transformer);
    }

    @Test
    public void testLifecycleInTransientRegistry() throws Exception
    {
        TransformerLifecycleTracker transformer = new TransformerLifecycleTracker();
        transformer.setProperty("foo");
        muleContext.getRegistry().registerTransformer(transformer);
        muleContext.dispose();
        // MULE-5829 Artifacts excluded from lifecycle in MuleContextLifecyclePhases
        // gets lifecycle when an object is registered.
        // assertNoLifecycle(transformer);
    }

    @Test
    public void testLifecycleInFlowTransientRegistry() throws Exception
    {
        Flow flow = new Flow("flow", muleContext);
        TransformerLifecycleTracker transformer = new TransformerLifecycleTracker();
        transformer.setProperty("foo");
        flow.setMessageProcessors(Collections.<MessageProcessor>singletonList(transformer));
        muleContext.getRegistry().registerFlowConstruct(flow);
        muleContext.dispose();
        assertLifecycle(transformer);
    }

    private void assertLifecycle(TransformerLifecycleTracker transformer)
    {
        assertEquals("[setProperty, initialise, dispose]", transformer.getTracker().toString());
    }

    private void assertInitialise(TransformerLifecycleTracker transformer)
    {
        assertEquals("[setProperty, initialise]", transformer.getTracker().toString());
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
            return property;
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

        @Override
        public void dispose()
        {
            tracker.add("dispose");
        }
    }
}
