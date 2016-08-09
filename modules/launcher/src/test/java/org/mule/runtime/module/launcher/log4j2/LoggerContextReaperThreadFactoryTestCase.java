/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.module.launcher.log4j2.LoggerContextReaperThreadFactory.THREAD_NAME;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LoggerContextReaperThreadFactoryTestCase extends AbstractMuleTestCase {

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
