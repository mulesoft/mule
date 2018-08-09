/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mule.ProxyRequestPathParsingBenchmark.LISTENER_PATH_KEY;
import static org.mule.ProxyRequestPathParsingBenchmark.REQUEST_PATH_KEY;
import static org.mule.ProxyRequestPathParsingBenchmark.pathGroups;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.ProxyRequestPathParsingBenchmark;
import org.mule.runtime.core.api.event.CoreEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProxyRequestPathParsingTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final ProxyRequestPathParsingBenchmark dummyObject = new ProxyRequestPathParsingBenchmark();

  @BeforeClass
  public static void setUp() throws Exception {
    dummyObject.setup();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    dummyObject.teardown();
  }

  private List<Method> getAnnotatedMethods() {
    List<Method> annotatedMethods = new ArrayList<>();
    Method[] allMethods = ProxyRequestPathParsingBenchmark.class.getDeclaredMethods();
    for (Method method : allMethods) {
      if (method.isAnnotationPresent(ProxyRequestPathParsingBenchmark.Method.class)) {
        annotatedMethods.add(method);
      }
    }
    return annotatedMethods;
  }

  @Test
  public void allMethodsWork() throws Exception {
    ProxyRequestPathParsingBenchmark dummyObject = new ProxyRequestPathParsingBenchmark();
    dummyObject.setup();
    for (Method method : getAnnotatedMethods()) {
      System.out.print("Testing: " + method.getName());
      for (ProxyRequestPathParsingBenchmark.PathGroup pathGroup : pathGroups) {
        CoreEvent event = CoreEvent.builder(testEvent()).addVariable(LISTENER_PATH_KEY, pathGroup.getListenerPath())
            .addVariable(REQUEST_PATH_KEY, pathGroup.getRequestPath()).build();
        String result = (String) method.invoke(dummyObject, event);
        assertThat(result, is(equalTo(pathGroup.getExpectedProxyRequestPath())));
      }
      System.out.println(" OK");
    }
  }


}
