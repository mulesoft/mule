/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.module.log4j.internal.LoggerContextReaperThreadFactory.THREAD_NAME;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class LoggerContextReaperThreadFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private ClassLoader contextClassLoader;

  @Mock
  private Runnable runnable;

  private LoggerContextReaperThreadFactory factory;
  private Thread thread;

  @Before
  public void before() {
    factory = new LoggerContextReaperThreadFactory(contextClassLoader);
    thread = factory.newThread(runnable);
  }

  @Test
  public void testProperties() throws Exception {
    assertThat(thread.getName(), is(THREAD_NAME));
    assertThat(thread.getContextClassLoader(), is(sameInstance(contextClassLoader)));
  }

  @Test
  public void runDelegatesIntoTheRightRunnable() {
    thread.run();
    verify(runnable).run();
  }
}
