/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.generics;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class GenericsUtilsTestCase extends AbstractMuleTestCase {

  protected Class<?> targetClass;

  protected String methods[];

  protected Type expectedResults[];

  @Before
  public void createTestData() {
    this.targetClass = Foo.class;
    this.methods = new String[] {"a", "b", "b2", "b3", "c", "d", "d2", "d3", "e", "e2", "e3"};
    this.expectedResults = new Class[] {Integer.class, null, Set.class, Set.class, null, Integer.class, Integer.class,
        Integer.class, Integer.class, Integer.class, Integer.class};
  }

  protected Type getType(Method method) {
    return GenericsUtils.getMapValueReturnType(method);
  }

  @Test
  public void testA() throws Exception {
    executeTest();
  }

  @Test
  public void testB() throws Exception {
    executeTest();
  }

  @Test
  public void testB2() throws Exception {
    executeTest();
  }

  @Test
  public void testB3() throws Exception {
    executeTest();
  }

  @Test
  public void testC() throws Exception {
    executeTest();
  }

  @Test
  public void testD() throws Exception {
    executeTest();
  }

  @Test
  public void testD2() throws Exception {
    executeTest();
  }

  @Test
  public void testD3() throws Exception {
    executeTest();
  }

  @Test
  public void testE() throws Exception {
    executeTest();
  }

  @Test
  public void testE2() throws Exception {
    executeTest();
  }

  @Test
  public void testE3() throws Exception {
    executeTest();
  }

  @Test
  public void testProgrammaticListIntrospection() throws Exception {
    Method setter = GenericBean.class.getMethod("setResourceList", List.class);
    assertEquals(String.class, GenericsUtils.getCollectionParameterType(new MethodParameter(setter, 0)));

    Method getter = GenericBean.class.getMethod("getResourceList");
    assertEquals(String.class, GenericsUtils.getCollectionReturnType(getter));
  }


  private abstract class CustomMap<T> extends AbstractMap<String, Integer> {
  }


  private abstract class OtherCustomMap<T> implements Map<String, Integer> {
  }


  private interface Foo {

    Map<String, Integer> a();

    Map<?, ?> b();

    Map<?, ? extends Set> b2();

    Map<?, ? super Set> b3();

    Map c();

    CustomMap<Date> d();

    CustomMap<?> d2();

    CustomMap d3();

    OtherCustomMap<Date> e();

    OtherCustomMap<?> e2();

    OtherCustomMap e3();
  }


  protected void executeTest() throws NoSuchMethodException {
    String methodName = name.getMethodName().trim().replaceFirst("test", "").toLowerCase();
    for (int i = 0; i < this.methods.length; i++) {
      if (methodName.equals(this.methods[i])) {
        Method method = this.targetClass.getMethod(methodName);
        Type type = getType(method);
        assertEquals(this.expectedResults[i], type);
        return;
      }
    }
    throw new IllegalStateException("Bad test data");
  }


}
