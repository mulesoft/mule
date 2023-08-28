/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.module.service.internal.manager.DefaultMethodInvoker;
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

  private final DefaultMethodInvoker defaultMethodInvoker = new DefaultMethodInvoker();

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
