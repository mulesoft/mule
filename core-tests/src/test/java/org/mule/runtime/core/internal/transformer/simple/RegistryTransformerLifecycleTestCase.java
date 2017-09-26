/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.api.construct.Flow.builder;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Highlights the issue: MULE-4599 where dispose cannot be called on a transformer since it is a prototype in Spring, so spring
 * does not manage the object.
 */
public class RegistryTransformerLifecycleTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testLifecycleInTransientRegistry() throws Exception {
    TransformerLifecycleTracker transformer = new TransformerLifecycleTracker();
    transformer.setProperty("foo");
    ((MuleContextWithRegistries) muleContext).getRegistry().registerTransformer(transformer);
    muleContext.dispose();
    // Artifacts excluded from lifecycle in MuleContextLifecyclePhase gets lifecycle when an object is registered.
    assertRegistrationOnlyLifecycle(transformer);
  }

  @Test
  public void testLifecycleInFlowTransientRegistry() throws Exception {
    TransformerLifecycleTracker transformer = new TransformerLifecycleTracker();
    transformer.setProperty("foo");
    Flow flow = builder("flow", muleContext).processors(transformer).build();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerFlowConstruct(flow);

    muleContext.dispose();
    assertLifecycle(transformer);
  }

  private void assertRegistrationOnlyLifecycle(TransformerLifecycleTracker transformer) {
    assertEquals("[setProperty, initialise]", transformer.getTracker().toString());
  }

  private void assertLifecycle(TransformerLifecycleTracker transformer) {
    assertEquals("[setProperty, initialise, dispose]", transformer.getTracker().toString());
  }

  public static class TransformerLifecycleTracker extends AbstractTransformer implements Disposable {

    private final List<String> tracker = new ArrayList<>();

    private String property;

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      tracker.add("doTransform");
      return null;
    }

    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      tracker.add("setProperty");
    }

    public List<String> getTracker() {
      return tracker;
    }

    @Override
    public void initialise() throws InitialisationException {
      tracker.add("initialise");
    }

    @Override
    public void dispose() {
      tracker.add("dispose");
    }
  }
}
