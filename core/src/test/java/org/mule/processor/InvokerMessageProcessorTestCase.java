/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InvokerMessageProcessorTestCase extends AbstractMuleContextTestCase
{

    private InvokerMessageProcessor invoker;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        invoker = new InvokerMessageProcessor();
        invoker.setObject(new TestInvokeObject());
        invoker.setMuleContext(muleContext);
    }

    @Test
    public void testMethodWithNoArgs() throws MuleException, Exception
    {
        invoker.setMethodName("testNoArgs");
        invoker.initialise();
        invoker.process(getTestEvent(""));
    }

    @Test
    public void testMethodFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod");
        invoker.setArgumentExpressionsString("#[string:1],#[string:2],#[string:3],#[string:4],#[string:5],#[string:6],#[string:7],#[string:8],#[string:true],#[string:true],#[string:11]");
        invoker.initialise();
        invoker.process(getTestEvent(""));
    }

    @Test
    public void testMethodFoundNestedExpression() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("#[string:#[string:1]]");
        invoker.initialise();
        assertEquals("1 echo", invoker.process(getTestEvent("")).getMessageAsString());
    }

    @Test
    public void testMethodFoundParseStringWithExpressions() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("1-#[string:#[string:2]]-3");
        invoker.initialise();
        assertEquals("1-2-3 echo", invoker.process(getTestEvent("")).getMessageAsString());
    }

    @Test
    public void testMethodFoundParseStringNoExpressions() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("1");
        invoker.initialise();
        assertEquals("1 echo", invoker.process(getTestEvent("")).getMessageAsString());
    }

    @Test
    public void testMethodFoundNullArgument() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArguments(Collections.singletonList(null));
        invoker.initialise();
        assertEquals("null echo", invoker.process(getTestEvent("")).getMessageAsString());
    }

    @Test
    public void testMethodNameNotFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethodNotHere");
        invoker.setArgumentExpressionsString("#[string:1]");
        try
        {
            invoker.initialise();
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(InitialisationException.class, e.getClass());
        }
    }

    @Test
    public void testMethodWithArgsNotFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod");
        invoker.setArgumentExpressionsString("#[string:1]");
        try
        {
            invoker.initialise();
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(InitialisationException.class, e.getClass());
        }
    }

    @Test
    public void testMethodWithArgTypes() throws MuleException, Exception
    {
        invoker.setMethodName("testDuplicateNameMethod");
        invoker.setArgumentExpressionsString("#[string:1], #[string:2]");
        invoker.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        invoker.initialise();
        assertEquals("12(string and int)", invoker.process(getTestEvent("")).getMessageAsString());

    }

    @Test
    public void testCantTransform() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod2");
        invoker.setArgumentExpressionsString("#[string:1]");
        invoker.initialise();
        try
        {
            invoker.process(getTestEvent(""));
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(MessagingException.class, e.getClass());
            assertEquals(TransformerException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testReplacePayload() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("#[payload:]");
        invoker.initialise();
        assertEquals("hello echo", invoker.process(getTestEvent("hello")).getMessageAsString());
    }

    @Test
    public void testArrayArg() throws MuleException, Exception
    {
        invoker.setMethodName("testArrayArg");
        invoker.setArguments(Collections.singletonList(new String[]{"#[string:1]", "#[string:2]"}));
        invoker.initialise();
        MuleEvent result = invoker.process(getTestEvent(""));
        assertEquals(String[].class, result.getMessage().getPayload().getClass());
        assertEquals("1", ((String[]) result.getMessage().getPayload())[0]);
        assertEquals("2", ((String[]) result.getMessage().getPayload())[1]);
    }

    @Test
    public void testListArg() throws MuleException, Exception
    {
        invoker.setMethodName("testListArg");
        invoker.setArguments(Collections.singletonList(Collections.singletonList("#[string:1]")));
        invoker.initialise();
        MuleEvent result = invoker.process(getTestEvent(""));
        assertTrue(List.class.isAssignableFrom(result.getMessage().getPayload().getClass()));
        assertEquals("1", ((List) result.getMessage().getPayload()).get(0));
    }

    @Test
    public void testListNestedMapArg() throws MuleException, Exception
    {
        invoker.setMethodName("testListArg");
        invoker.setArguments(Collections.singletonList(Collections.singletonList(Collections.singletonMap(
            "#[string:key]", "#[string:val]"))));
        invoker.initialise();
        MuleEvent result = invoker.process(getTestEvent(""));
        assertTrue(List.class.isAssignableFrom(result.getMessage().getPayload().getClass()));
        assertEquals("val", ((Map) ((List) result.getMessage().getPayload()).get(0)).get("key"));
    }

    @Test
    public void testMapArg() throws MuleException, Exception
    {
        invoker.setMethodName("testMapArg");
        invoker.setArguments(Collections.singletonList(Collections.singletonMap("#[string:key]",
            "#[string:val]")));
        invoker.initialise();
        MuleEvent result = invoker.process(getTestEvent(""));
        assertTrue(Map.class.isAssignableFrom(result.getMessage().getPayload().getClass()));
        assertEquals("val", ((Map) result.getMessage().getPayload()).get("key"));
    }

    @Test
    public void testLookupClassInstance() throws MuleException, Exception
    {
        muleContext.getRegistry().registerObject("object", new TestInvokeObject());

        invoker = new InvokerMessageProcessor();
        invoker.setMuleContext(muleContext);
        invoker.setObjectType(TestInvokeObject.class);
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("#[string:1]");
        invoker.initialise();
        assertEquals("1 echo", invoker.process(getTestEvent("")).getMessageAsString());
    }

    private class TestInvokeObject
    {

        public void testMethod(Integer arg1,
                               int arg2,
                               Long arg3,
                               long arg4,
                               Double arg5,
                               double arg6,
                               Float arg7,
                               float arg8,
                               Boolean arg9,
                               boolean arg10,
                               String arg11)
        {
        }

        public void testNoArgs()
        {
        }

        public void testMethod2(Apple apple)
        {

        }

        public String testMethod3(String text)
        {
            return text + " echo";
        }

        public String testDuplicateNameMethod(String text, String text2)
        {
            return text + text2 + " (two strings)";
        }

        public String testDuplicateNameMethod(String text, int i)
        {
            return text + i + "(string and int)";
        }

        public String[] testArrayArg(String[] array)
        {
            return array;
        }

        public List<String> testListArg(List<String> list)
        {
            return list;
        }

        public Map<String, String> testMapArg(Map<String, String> map)
        {
            return map;
        }

    }

}
