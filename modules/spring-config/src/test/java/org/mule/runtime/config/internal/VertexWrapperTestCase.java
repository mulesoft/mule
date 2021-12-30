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
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cglib.proxy.Proxy;
import scala.util.matching.Regex;
import sun.misc.ProxyGenerator;

import static org.hamcrest.MatcherAssert.assertThat;

public class VertexWrapperTestCase {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void twoDifferentInstanceTest() {
    // given
    // 2 wrappers - 2 different instances (false)
    // proxy (true) / same proxy: proxy vs. proxy(true)
    Object object1 = new Object();
    Object object2 = new Object();
    VertexWrapper first = new VertexWrapper(object1);
    VertexWrapper second = new VertexWrapper(object2);
    // when
    // then
    assertThat(first.equals(second), Matchers.is(false));
  }

  @Test
  public void sameInstanceOneProxyTest() {
    // 2 wrappers - proxy vs. original (true) / different proxies same class inside: proxy vs.
    Object object1 = new Object();
    VertexWrapper first = new VertexWrapper(object1);

    assertThat(first.equals(object1), Matchers.is(true));
  }

  public void proxyComparisonTest() {

  }

  @Test
  public void fakeProxyTest() {
    FakeProxy fProxy = new FakeProxy(new Object());
    assertThat(fProxy.equals(fProxy), Matchers.is(false));
  }

  @Test
  public void fakeProxyWrapperTest() {

    FakeProxy fProxy = new FakeProxy(new Object());
    VertexWrapper wrapper = new VertexWrapper(fProxy);

    assertThat(wrapper.equals(wrapper), Matchers.is(true));
  }

  @Test
  public void fakeProxyWrapperTest2() {

    FakeProxy fProxy = new FakeProxy(new Object());
    VertexWrapper wrapper1 = new VertexWrapper(fProxy);
    VertexWrapper wrapper2 = new VertexWrapper(fProxy);

    assertThat(wrapper1.equals(wrapper2), Matchers.is(true));
    assertThat(wrapper2.equals(wrapper1), Matchers.is(true));
  }

  @Test
  public void fakeProxyWrapperTest3() {
    Object obj = new Object();
    FakeProxy fProxy1 = new FakeProxy(obj);
    FakeProxy fProxy2 = new FakeProxy(obj);
    VertexWrapper wrapper1 = new VertexWrapper(fProxy1);
    VertexWrapper wrapper2 = new VertexWrapper(fProxy2);

    assertThat(wrapper1.equals(wrapper2), Matchers.is(true));
    assertThat(wrapper2.equals(wrapper1), Matchers.is(true));
  }

  @Test
  public void nullValueWrapperTest() {
    exceptionRule.expect(NullPointerException.class);
    exceptionRule.expectMessage("bean should not be null");
    VertexWrapper wrapper = new VertexWrapper(null);
  }

  @Test
  public void differentObjectsTest() {
    Object object1 = new Object();
    VertexWrapper first = new VertexWrapper(object1);
    Object object2 = new Object();
    VertexWrapper second = new VertexWrapper(object2);
    assertThat(first.equals(second), Matchers.is(false));
  }

  @Test
  public void differentObjectsTest2() {
    Object object1 = new Object();
    VertexWrapper first = new VertexWrapper(object1);
    Object object2 = new Object();
    assertThat(first.equals(object2), Matchers.is(false));
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
