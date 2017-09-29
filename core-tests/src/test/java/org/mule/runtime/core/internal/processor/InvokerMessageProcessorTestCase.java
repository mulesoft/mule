/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InvokerMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private InvokerMessageProcessor invoker;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    invoker = new InvokerMessageProcessor();
    invoker.setObject(new TestInvokeObject());
    invoker.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    invoker.setMuleContext(muleContext);
  }

  @Test
  public void testMethodWithNoArgs() throws MuleException, Exception {
    invoker.setMethodName("testNoArgs");
    invoker.initialise();
    invoker.process(testEvent());
  }

  @Test
  public void testMethodFound() throws MuleException, Exception {
    invoker.setMethodName("testMethod");
    invoker
        .setArgumentExpressionsString("#[mel:'1'],#[mel:'2'],#[mel:'3'],#[mel:'4'],#[mel:'5'],#[mel:'6'],#[mel:'7'],#[mel:'8'],#[mel:'true'],#[mel:'true'],#[mel:'1']");
    invoker.initialise();
    invoker.process(testEvent());
  }

  @Test
  public void testMethodFoundNestedExpression() throws MuleException, Exception {
    invoker.setMethodName("testMethod3");
    invoker.setArgumentExpressionsString("#[mel:#[mel:'1']]");
    invoker.initialise();
    assertEquals("1 echo", ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  @Test
  public void testMethodFoundParseStringWithExpressions() throws MuleException, Exception {
    invoker.setMethodName("testMethod3");
    invoker.setArgumentExpressionsString("1-#[mel:#[mel:'2']]-3");
    invoker.initialise();
    assertEquals("1-2-3 echo",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  @Test
  public void testMethodFoundParseStringNoExpressions() throws MuleException, Exception {
    invoker.setMethodName("testMethod3");
    invoker.setArgumentExpressionsString("1");
    invoker.initialise();
    assertEquals("1 echo",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  @Test
  public void testMethodFoundNullArgument() throws MuleException, Exception {
    invoker.setMethodName("testMethod3");
    invoker.setArguments(Collections.singletonList(null));
    invoker.initialise();
    assertEquals("null echo",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  @Test
  public void testMethodNameNotFound() throws MuleException, Exception {
    invoker.setMethodName("testMethodNotHere");
    invoker.setArgumentExpressionsString("#[mel:'1']");
    try {
      invoker.initialise();
      fail("Exception expected");
    } catch (Exception e) {
      assertEquals(InitialisationException.class, e.getClass());
    }
  }

  @Test
  public void testMethodWithArgsNotFound() throws MuleException, Exception {
    invoker.setMethodName("testMethod");
    invoker.setArgumentExpressionsString("#[mel:'1']");
    try {
      invoker.initialise();
      fail("Exception expected");
    } catch (Exception e) {
      assertEquals(InitialisationException.class, e.getClass());
    }
  }

  @Test
  public void testMethodWithArgTypes() throws MuleException, Exception {
    invoker.setMethodName("testDuplicateNameMethod");
    invoker.setArgumentExpressionsString("#[mel:'1'], #[mel:'2']");
    invoker.setArgumentTypes(new Class[] {String.class, Integer.TYPE});
    invoker.initialise();
    assertEquals("12(string and int)",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));

  }

  @Test
  public void testMethodWithPrimitiveArgTypes() throws MuleException, Exception {
    invoker.setMethodName("testDuplicateNameMethod");
    invoker.setArgumentTypes(new Class<?>[] {String.class, Integer.class});
    invoker.setArguments(asList("some String", 42));
    invoker.initialise();
    assertEquals("some String42(string and int)",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));

  }

  @Test
  public void testCantTransform() throws MuleException, Exception {
    invoker.setMethodName("testMethod2");
    invoker.setArgumentExpressionsString("#[mel:'1']");
    invoker.initialise();
    try {
      invoker.process(testEvent());
      fail("Exception expected");
    } catch (Exception e) {
      assertEquals(MessagingException.class, e.getClass());
      assertEquals(TransformerException.class, e.getCause().getClass());
    }
  }

  @Test
  public void testReplacePayload() throws MuleException, Exception {
    invoker.setMethodName("testMethod3");
    invoker.setArgumentExpressionsString("#[mel:payload:]");
    invoker.initialise();
    assertEquals(TEST_PAYLOAD + " echo", ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  @Test
  public void testArrayArg() throws MuleException, Exception {
    invoker.setMethodName("testArrayArg");
    invoker.setArguments(Collections.singletonList(new String[] {"#[mel:'1']", "#[mel:'2']"}));
    invoker.initialise();
    CoreEvent result = invoker.process(testEvent());
    assertEquals(String[].class, result.getMessage().getPayload().getDataType().getType());
    assertEquals("1", ((String[]) result.getMessage().getPayload().getValue())[0]);
    assertEquals("2", ((String[]) result.getMessage().getPayload().getValue())[1]);
  }

  @Test
  public void testListArg() throws MuleException, Exception {
    invoker.setMethodName("testListArg");
    invoker.setArguments(Collections.singletonList(Collections.singletonList("#[mel:'1']")));
    invoker.initialise();
    CoreEvent result = invoker.process(testEvent());
    assertTrue(List.class.isAssignableFrom(result.getMessage().getPayload().getDataType().getType()));
    assertEquals("1", ((List) result.getMessage().getPayload().getValue()).get(0));
  }

  @Test
  public void testListNestedMapArg() throws MuleException, Exception {
    invoker.setMethodName("testListArg");
    invoker.setArguments(Collections
        .singletonList(Collections.singletonList(Collections.singletonMap("#[mel:'key']", "#[mel:'val']"))));
    invoker.initialise();
    CoreEvent result = invoker.process(testEvent());
    assertTrue(List.class.isAssignableFrom(result.getMessage().getPayload().getDataType().getType()));
    assertEquals("val", ((Map) ((List) result.getMessage().getPayload().getValue()).get(0)).get("key"));
  }

  @Test
  public void testMapArg() throws MuleException, Exception {
    invoker.setMethodName("testMapArg");
    invoker.setArguments(Collections.singletonList(Collections.singletonMap("#[mel:'key']", "#[mel:'val']")));
    invoker.initialise();
    CoreEvent result = invoker.process(testEvent());
    assertTrue(Map.class.isAssignableFrom(result.getMessage().getPayload().getDataType().getType()));
    assertEquals("val", ((Map) result.getMessage().getPayload().getValue()).get("key"));
  }

  @Test
  public void testLookupClassInstance() throws MuleException, Exception {
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("object", new TestInvokeObject());

    invoker = new InvokerMessageProcessor();
    invoker.setMuleContext(muleContext);
    invoker.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    invoker.setObjectType(TestInvokeObject.class);
    invoker.setMethodName("testMethod3");
    invoker.setArgumentExpressionsString("#[mel:'1']");
    invoker.initialise();
    assertEquals("1 echo",
                 ((PrivilegedEvent) invoker.process(testEvent())).getMessageAsString(muleContext));
  }

  private class TestInvokeObject {

    public void testMethod(Integer arg1, int arg2, Long arg3, long arg4, Double arg5, double arg6, Float arg7, float arg8,
                           Boolean arg9, boolean arg10, String arg11) {}

    public void testNoArgs() {}

    public void testMethod2(Apple apple) {

    }

    public String testMethod3(String text) {
      return text + " echo";
    }

    public String testDuplicateNameMethod(String text, String text2) {
      return text + text2 + " (two strings)";
    }

    public String testDuplicateNameMethod(String text, int i) {
      return text + i + "(string and int)";
    }

    public String[] testArrayArg(String[] array) {
      return array;
    }

    public List<String> testListArg(List<String> list) {
      return list;
    }

    public Map<String, String> testMapArg(Map<String, String> map) {
      return map;
    }

  }

}
