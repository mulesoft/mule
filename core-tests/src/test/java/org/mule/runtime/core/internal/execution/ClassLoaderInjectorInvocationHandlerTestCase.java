/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.execution;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.execution.ClassLoaderInjectorInvocationHandler.createClassLoaderInjectorInvocationHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

@SmallTest
public class ClassLoaderInjectorInvocationHandlerTestCase extends AbstractMuleTestCase {


  @Test
  public void delegatesMethodInvocation() throws Exception {
    TestDelegate delegate = mock(TestDelegate.class);
    ClassLoader classLoader = getContextClassLoader();

    TestDelegate proxy = (TestDelegate) createClassLoaderInjectorInvocationHandler(delegate, classLoader);

    proxy.doStuff();

    verify(delegate).doStuff();
  }

  @Test
  public void usesClassLoaderOnMethodDelegation() throws Exception {
    final ClassLoader classLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());

    TestDelegate delegate = mock(TestDelegate.class);
    doAnswer(invocation -> {
      assertThat(getContextClassLoader(), equalTo(classLoader));

      return null;
    }).when(delegate).doStuff();

    TestDelegate proxy = (TestDelegate) createClassLoaderInjectorInvocationHandler(delegate, classLoader);

    proxy.doStuff();
  }

  @Test
  public void restoresOriginalClassLoaderAfterMethodDelegation() throws Exception {
    ClassLoader originalClassLoader = getContextClassLoader();

    TestDelegate delegate = mock(TestDelegate.class);

    final ClassLoader classLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());

    createClassLoaderInjectorInvocationHandler(delegate, classLoader);

    assertSame(originalClassLoader, getContextClassLoader());
  }

  private ClassLoader getContextClassLoader() {
    return currentThread().getContextClassLoader();
  }

  public interface TestDelegate {

    void doStuff();
  }

}
