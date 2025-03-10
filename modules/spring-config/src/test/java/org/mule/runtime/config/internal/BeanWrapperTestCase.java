/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.newProxyInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.api.MuleContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("MULE-19984")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class BeanWrapperTestCase {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void twoDifferentObjectsComparisonTest() {

    Object object1 = new Object();
    Object object2 = new Object();
    BeanWrapper first = new BeanWrapper("firstObject", object1);
    BeanWrapper second = new BeanWrapper("secondObject", object2);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void sameObjectInWrapperComparisonTest() {
    Object object1 = new Object();
    BeanWrapper first = new BeanWrapper("firstObject", object1);

    assertThat(first.equals(object1), is(true));
  }

  @Test
  public void sameProxyComparisonTest() {
    Object fProxy = newProxyInstance(currentThread().getContextClassLoader(), new Class[] {MuleContext.class},
                                     new MyInvocationHandler(new Object()));
    assertThat(fProxy.equals(fProxy), is(false));
  }

  @Test
  public void sameProxyInWrapperComparisonTest() {
    Object fProxy = newProxyInstance(currentThread().getContextClassLoader(), new Class[] {MuleContext.class},
                                     new MyInvocationHandler(new Object()));
    BeanWrapper wrapper = new BeanWrapper("proxy", fProxy);

    assertThat(wrapper.equals(wrapper), is(true));
  }

  @Test
  public void sameProxyInTwoDifferentWrappersComparisonTest() {
    Object fProxy = newProxyInstance(currentThread().getContextClassLoader(), new Class[] {MuleContext.class},
                                     new MyInvocationHandler(new Object()));
    BeanWrapper wrapper1 = new BeanWrapper("proxy1", fProxy);
    BeanWrapper wrapper2 = new BeanWrapper("proxy2", fProxy);

    assertThat(wrapper1.equals(wrapper2), is(true));
    assertThat(wrapper2.equals(wrapper1), is(true));
  }

  @Test
  public void objectInProxyAndWrapperComparisonTest() {
    Object obj = new Object();
    Object fProxy1 =
        newProxyInstance(currentThread().getContextClassLoader(), new Class[] {MuleContext.class}, new MyInvocationHandler(obj));
    Object fProxy2 =
        newProxyInstance(currentThread().getContextClassLoader(), new Class[] {MuleContext.class}, new MyInvocationHandler(obj));

    BeanWrapper wrapper1 = new BeanWrapper("proxy1", fProxy1);
    BeanWrapper wrapper2 = new BeanWrapper("proxy2", fProxy2);

    assertThat(wrapper1.equals(wrapper2), is(true));
    assertThat(wrapper2.equals(wrapper1), is(true));
  }

  @Test
  public void nullInWrapperTest() {
    exceptionRule.expect(NullPointerException.class);
    exceptionRule.expectMessage("bean `nullBean` must not be null");
    new BeanWrapper("nullBean", null);
  }

  @Test
  public void twoDifferentObjectsInWrapperTest() {
    Object object1 = new Object();
    Object object2 = new Object();
    BeanWrapper first = new BeanWrapper("firstObject", object1);
    BeanWrapper second = new BeanWrapper("secondObject", object2);
    assertThat(first.equals(second), is(false));
  }

  @Test
  public void differentObjectsInWrapperTest() {
    Object object1 = new Object();
    Object object2 = new Object();
    BeanWrapper first = new BeanWrapper("firstObject", object1);

    assertThat(first.equals(object2), is(false));
  }

  @Test
  public void getBasicInfoTest() {

    Object object1 = new Object();
    Object object2 = new Object();
    BeanWrapper first = new BeanWrapper("firstObject", object1);
    BeanWrapper second = new BeanWrapper("secondObject", object2);

    assertThat(first.getName().equals("firstObject"), is(true));
    assertThat(first.hashCode() == second.hashCode(), is(false));
  }


  private static class MyInvocationHandler implements InvocationHandler {

    Object wrappedObject;

    public MyInvocationHandler(Object obj) {
      this.wrappedObject = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("equals")) {
        return wrappedObject.equals(args[0]);
      }
      if (method.getName().equals("hashCode")) {
        return wrappedObject.hashCode();
      }
      return null;
    }
  }
}
