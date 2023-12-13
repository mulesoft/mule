/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_APPLY_OBJECT_PROCESSOR_PROPERTY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test lifecycle behaviour and restrictions on lifecyce methods
 */
// TODO W-10781591 Remove this test
public class JSR250ObjectLifcycleTestCase extends AbstractMuleContextTestCase {

  // This tests specifically tests features provided by ObjectProcessors
  @Rule
  public SystemProperty disableApplyObjectProcessor = new SystemProperty(DISABLE_APPLY_OBJECT_PROCESSOR_PROPERTY, "false");

  @Test
  public void testNormalBehaviour() throws Exception {
    JSR250ObjectLifecycleTracker tracker = new JSR250ObjectLifecycleTracker();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("test", tracker);

    muleContext.dispose();
    assertEquals("[setMuleContext, initialise, dispose]", tracker.getTracker().toString());
  }

  @Test
  public void testTwoPostConstructAnnotations() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("test",
                                                                           new DupePostConstructJSR250ObjectLifecycleTracker());
      fail("Object has two @PostConstruct annotations");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testTwoPreDestroyAnnotations() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("test",
                                                                           new DupePreDestroyJSR250ObjectLifecycleTracker());
      fail("Object has two @PreDestroy annotations");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBadReturnTypePostConstructMethod() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry()
          .registerObject("test", new BadReturnTypePostConstructLifecycleMethodObject());
      fail("PostContruct Lifecycle method has a non-void return type");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBadParamPreDestroyMethod() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("test",
                                                                           new BadParamPreDestroyLifecycleMethodObject());
      fail("PreDestroy Lifecycle method has a parameter");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBadStaticPreDestroyMethod() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry()
          .registerObject("test", new BadStaticMethodPostConstructLifecycleMethodObject());
      fail("PostConstruct Lifecycle method is static");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBadCheckedExceptionPreDestroyMethod() throws Exception {
    try {
      ((MuleContextWithRegistry) muleContext).getRegistry()
          .registerObject("test", new BadCheckedExceptionPreDestroyLifecycleMethodObject());
      fail("PreDestroy Lifecycle method throws a checked exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public class DupePostConstructJSR250ObjectLifecycleTracker extends JSR250ObjectLifecycleTracker {

    // You cannot have an object with two {@link PostConstruct} annotated methods
    @PostConstruct
    public void init2() {
      getTracker().add("initialise 2");
    }
  }

  public class DupePreDestroyJSR250ObjectLifecycleTracker extends JSR250ObjectLifecycleTracker {

    // You cannot have an object with two {@link PostConstruct} annotated methods
    @PreDestroy
    public void dispose2() {
      getTracker().add("dispose 2");
    }
  }

  public class BadReturnTypePostConstructLifecycleMethodObject {

    @PostConstruct
    public boolean init() {
      return true;
    }
  }

  public class BadParamPreDestroyLifecycleMethodObject {

    @PreDestroy
    public void destroy(boolean foo) {

    }
  }

  public static class BadStaticMethodPostConstructLifecycleMethodObject {

    @PostConstruct
    public static void init() {

    }
  }

  public class BadCheckedExceptionPreDestroyLifecycleMethodObject {

    @PreDestroy
    public void destroy() throws Exception {

    }
  }
}
