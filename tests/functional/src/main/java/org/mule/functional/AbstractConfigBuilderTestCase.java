/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.interceptor.InterceptorStack;
import org.mule.runtime.core.interceptor.LoggingInterceptor;
import org.mule.runtime.core.interceptor.TimerInterceptor;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;

import org.junit.Test;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase {

  public AbstractConfigBuilderTestCase(boolean legacy) {
    super(legacy);
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Override
  public void testManagerConfig() throws Exception {
    super.testManagerConfig();

    assertNotNull(muleContext.getTransactionManager());
  }

  @Test
  public void testExceptionStrategy2() {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent");
    assertNotNull(flow.getExceptionListener());
    assertTrue(flow.getExceptionListener() instanceof MessagingExceptionHandler);
  }

  @Override
  public void testTransformerConfig() {
    super.testTransformerConfig();

    Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
    assertNotNull(t);
    assertTrue(t instanceof TestCompressionTransformer);
    assertEquals(t.getReturnDataType(), DataType.STRING);
    assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
  }

  @Test
  public void testPoolingConfig() {
    // test per-descriptor overrides
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
    PoolingProfile pp = ((PooledJavaComponent) flow.getMessageProcessors().get(0)).getPoolingProfile();

    assertEquals(9, pp.getMaxActive());
    assertEquals(6, pp.getMaxIdle());
    assertEquals(4002, pp.getMaxWait());
    assertEquals(PoolingProfile.WHEN_EXHAUSTED_FAIL, pp.getExhaustedAction());
    assertEquals(PoolingProfile.INITIALISE_ALL, pp.getInitialisationPolicy());
  }

  @Test
  public void testEnvironmentProperties() {
    assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
    assertEquals("this was set from the manager properties!", muleContext.getRegistry().lookupObject("beanProperty1"));
    assertNotNull(muleContext.getRegistry().lookupObject("OS_Version"));
  }

  @Test
  public void testMuleConfiguration() {
    assertEquals(10, muleContext.getConfiguration().getDefaultResponseTimeout());
    assertEquals(20, muleContext.getConfiguration().getDefaultTransactionTimeout());
    assertEquals(30, muleContext.getConfiguration().getShutdownTimeout());
  }

  @Test
  public void testGlobalInterceptorStack() {
    InterceptorStack interceptorStack = (InterceptorStack) muleContext.getRegistry().lookupObject("testInterceptorStack");
    assertNotNull(interceptorStack);
    assertEquals(3, interceptorStack.getInterceptors().size());
    assertEquals(LoggingInterceptor.class, interceptorStack.getInterceptors().get(0).getClass());
    assertEquals(TimerInterceptor.class, interceptorStack.getInterceptors().get(1).getClass());
    assertEquals(LoggingInterceptor.class, interceptorStack.getInterceptors().get(2).getClass());
  }

}
