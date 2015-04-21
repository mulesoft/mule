/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class InboundHeadersAnnotationTestCase extends AbstractServiceAndFlowTestCase
{
    private Map<String, Object> props;

    public InboundHeadersAnnotationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/annotations/inbound-headers-annotation-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/annotations/inbound-headers-annotation-flow.xml"}
        });
    }

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();

        props = new HashMap<String, Object>(3);
        props.put("foo", "fooValue");
        props.put("bar", "barValue");
        props.put("baz", "bazValue");
    }

    @Test
    public void testSingleHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://header", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals("fooValue", message.getPayload());
    }

    @Test
    public void testSingleHeaderOptional() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headerOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals("faz not set", message.getPayload());
    }

    @Test
    public void testSingleHeaderWithType() throws Exception
    {
        Apple apple = new Apple();
        props.put("apple", apple);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headerWithType", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals(apple, message.getPayload());
    }

    @Test
    public void testSingleHeaderWithBaseType() throws Exception
    {
        Apple apple = new Apple();
        props.put("apple", apple);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headerWithBaseType", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals(apple, message.getPayload());
    }

    @Test
    public void testMapHeaders() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headers", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapHeadersMissing() throws Exception
    {
        props.remove("foo");

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headers", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(RequiredValueException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }

    @Test
    public void testMapSingleHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://singleHeaderMap", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(1, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertNull(result.get("bar"));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapHeadersOptional() throws Exception
    {
        props.remove("baz");

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapHeadersAllOptional() throws Exception
    {
        props.clear();

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersAllOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //We just wan tot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    @Test
    public void testMapHeadersUnmodifiable() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersUnmodifiable", null, props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(UnsupportedOperationException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }

    @Test
    public void testMapHeadersAll() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersAll", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //Will include all Mule headers too
        assertTrue(result.size() >= 3);
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertEquals("bazValue", result.get("baz"));
    }

    @Test
    public void testMapHeadersWildcard() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        printResult(result);
        //Will match all Mule headers
        assertEquals(3, result.size());
        assertEquals("vm://headersWildcard", result.get(MuleProperties.MULE_ENDPOINT_PROPERTY));
        assertTrue(result.keySet().contains(MuleProperties.MULE_SESSION_PROPERTY));
    }

    @Test
    public void testMapHeadersMultiWildcard() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersMultiWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        printResult(result);
        //Will match all Mule headers
        assertEquals(5, result.size());

        //Match on MULE_*
        assertEquals("vm://headersMultiWildcard", result.get(MuleProperties.MULE_ENDPOINT_PROPERTY));
        assertTrue(result.keySet().contains(MuleProperties.MULE_SESSION_PROPERTY));

        //Match on ba*
        assertEquals(result.get("bar"), "barValue");
        assertEquals(result.get("baz"), "bazValue");

    }

    @Test
    public void testMapHeadersWithGenerics() throws Exception
    {
        props.put("apple", new Apple());
        props.put("banana", new Banana());
        props.put("orange", new Orange());

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersWithGenerics", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //Will match all Mule headers
        assertEquals(2, result.size());

        assertEquals(new Apple(), result.get("apple"));
        assertEquals(new Orange(), result.get("orange"));
        assertNull(result.get("banana"));
    }

    @Test
    public void testListHeaders() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersList", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(3, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
        assertTrue(result.contains("bazValue"));
    }

    @Test
    public void testListHeadersWithOptional() throws Exception
    {
        props.remove("baz");
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(2, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
    }

    @Test
    public void testListHeadersWithMissing() throws Exception
    {
        props.remove("bar");

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListOptional", null, props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(RequiredValueException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());

    }

    @Test
    public void testSingleListHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://singleHeaderList", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(1, result.size());
        assertTrue(result.contains("fooValue"));
    }

    @Test
    public void testListHeadersUnmodifiable() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListUnmodifiable", null, props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(UnsupportedOperationException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }

    @Test
    public void testListHeadersAll() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListAll", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //Will include all Mule headers too
        assertTrue(result.size() >= 3);
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
        assertTrue(result.contains("bazValue"));
    }

    @Test
    public void testMapHeadersListAllOptional() throws Exception
    {
        props.clear();

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListAllOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //We just want ot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    @Test
    public void testListHeadersWilcard() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        printResult(result);
        //Will match all Mule headers
        assertEquals(3, result.size());

        //MULE_ENDPOINT
        assertTrue(result.contains("vm://headersListWildcard"));
        //The last value is the encoded session
    }

    @Test
    public void testListHeadersMultiWilcard() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListMultiWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        printResult(result);
        //Will match on MULE_* and ba*
        assertEquals(5, result.size());

        //Match on MULE_*

        //MULE_ENDPOINT
        assertTrue(result.contains("vm://headersListMultiWildcard"));
        //The last value is the encoded session

        //Match on ba*
        //bar
        assertTrue(result.contains("barValue"));

        //baz
        assertTrue(result.contains("bazValue"));
    }

    @Test
    public void testListHeadersWithGenerics() throws Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        props.put("apple", apple);
        props.put("banana", banana);
        props.put("orange", orange);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://headersListWithGenerics", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //Will match all Mule headers
        assertEquals(2, result.size());

        assertTrue(result.contains(apple));
        assertTrue(result.contains(orange));
        assertFalse(result.contains(banana));
    }

    public void printResult(List<?> result)
    {
        for(int i = 0; i < result.size(); i++)
        {
            System.out.println("result #" + i + ": " + result.get(i));
        }
    }

    public void printResult(Map<?, ?> result)
    {
        Set<?> keys = result.keySet();         // The set of keys in the map.
        Iterator<?> keyIter = keys.iterator();
        System.out.println("The map contains the following associations:");
        while (keyIter.hasNext())
        {
           Object key = keyIter.next();  // Get the next key.
           Object value = result.get(key);  // Get the value for that key.
           System.out.println( "   (" + key + "," + value + ")" );
        }
    }
}
