/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VertexWrapperTestCase {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void twoDifferentObjectsComparisonTest() {

    Object object1 = new Object();
    Object object2 = new Object();
    VertexWrapper first = new VertexWrapper("firstObject", object1);
    VertexWrapper second = new VertexWrapper("secondObject", object2);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void sameObjectInWrapperComparisonTest() {
    Object object1 = new Object();
    VertexWrapper first = new VertexWrapper("firstObject", object1);

    assertThat(first.equals(object1), is(true));
  }

  @Test
  public void sameProxyComparisonTest() {
    FakeProxy fProxy = new FakeProxy(new Object());
    assertThat(fProxy.equals(fProxy), is(false));
  }

  @Test
  public void sameProxyInWrapperComparisonTest() {
    FakeProxy fProxy = new FakeProxy(new Object());
    VertexWrapper wrapper = new VertexWrapper("proxy", fProxy);

    assertThat(wrapper.equals(wrapper), is(true));
  }

  @Test
  public void sameProxyInTwoDifferentWrappersComparisonTest() {
    FakeProxy fProxy = new FakeProxy(new Object());
    VertexWrapper wrapper1 = new VertexWrapper("proxy1", fProxy);
    VertexWrapper wrapper2 = new VertexWrapper("proxy2", fProxy);

    assertThat(wrapper1.equals(wrapper2), is(true));
    assertThat(wrapper2.equals(wrapper1), is(true));
  }

  @Test
  public void objectInProxyAndWrapperComparisonTest() {
    Object obj = new Object();
    FakeProxy fProxy1 = new FakeProxy(obj);
    FakeProxy fProxy2 = new FakeProxy(obj);
    VertexWrapper wrapper1 = new VertexWrapper("proxy1", fProxy1);
    VertexWrapper wrapper2 = new VertexWrapper("proxy2", fProxy2);

    assertThat(wrapper1.equals(wrapper2), is(true));
    assertThat(wrapper2.equals(wrapper1), is(true));
  }

  @Test
  public void nullInWrapperTest() {
    exceptionRule.expect(NullPointerException.class);
    exceptionRule.expectMessage("bean should not be null");
    new VertexWrapper("null", null);
  }

  @Test
  public void twoDifferentObjectsInWrapperTest() {
    Object object1 = new Object();
    Object object2 = new Object();
    VertexWrapper first = new VertexWrapper("firstObject", object1);
    VertexWrapper second = new VertexWrapper("secondObject", object2);
    assertThat(first.equals(second), is(false));
  }

  @Test
  public void differentObjectsInWrapperTest() {
    Object object1 = new Object();
    Object object2 = new Object();
    VertexWrapper first = new VertexWrapper("firstObject", object1);

    assertThat(first.equals(object2), is(false));
  }


  static class FakeProxy {

    Object originalObject;

    public FakeProxy(Object originalObject) {
      this.originalObject = originalObject;
    }

    @Override
    public int hashCode() {
      return this.originalObject.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return originalObject.equals(obj);
    }
  }

}
