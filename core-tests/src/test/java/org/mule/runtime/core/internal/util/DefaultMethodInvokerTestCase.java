/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultMethodInvokerTestCase extends AbstractMuleTestCase {

  private static final String MESSAGE = "someMessage";
  private static final String TEST_VALUE = "someValue";

  @Rule
  public ExpectedException expectedException = none();

  private DefaultMethodInvoker defaultMethodInvoker = new DefaultMethodInvoker();

  @Test
  public void callCorrectMethod() throws Throwable {
    TestObject object = new TestObject();
    Method method = TestObject.class.getMethod("doSomething");

    Object result = defaultMethodInvoker.invoke(object, method, new Object[0]);

    assertThat(result, is(TEST_VALUE));
  }

  @Test
  public void throwRealException() throws Throwable {
    TestObject object = new TestObject();
    Method method = TestObject.class.getMethod("throwSomething");

    expectedException.expect(IOException.class);
    expectedException.expectMessage(MESSAGE);

    defaultMethodInvoker.invoke(object, method, new Object[0]);
  }

  public class TestObject {

    public String doSomething() {
      return TEST_VALUE;
    }

    public String throwSomething() throws IOException {
      throw new IOException(MESSAGE);
    }
  }
}
