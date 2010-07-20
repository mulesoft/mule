/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboundHeadersAnnotationTestCase extends FunctionalTestCase
{
    private Map<String, Object> props;

    public InboundHeadersAnnotationTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/annotations/inbound-headers-annotation.xml";
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

    public void testSingleHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://header", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals("fooValue", message.getPayload());
    }

    public void testSingleHeaderOptional() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headerOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals("faz not set", message.getPayload());
    }


    public void testSingleHeaderWithType() throws Exception
    {
        Apple apple = new Apple();
        props.put("apple", apple);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headerWithType", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals(apple, message.getPayload());
    }

    public void testSingleHeaderWithBaseType() throws Exception
    {
        Apple apple = new Apple();
        props.put("apple", apple);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headerWithBaseType", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertEquals(apple, message.getPayload());
    }

    public void testMapHeaders() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headers", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersMissing() throws Exception
    {
        props.remove("foo");
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headers", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof RequiredValueException);
    }

    public void testMapSingleHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://singleHeaderMap", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(1, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertNull(result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersOptional() throws Exception
    {
        props.remove("baz");

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersAllOptional() throws Exception
    {
        props.clear();

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersAllOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //We just wan tot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    public void testMapHeadersUnmodifiable() throws Exception
    {

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersUnmodifiable", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertNotNull("Exception should have been thrown", message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof UnsupportedOperationException);
    }

    public void testMapHeadersAll() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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

    public void testMapHeadersWildcard() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //Will match all Mule headers
        assertEquals(3, result.size());
        assertEquals(result.get(MuleProperties.MULE_ENCODING_PROPERTY), "UTF-8");
        assertEquals(result.get(MuleProperties.MULE_ENDPOINT_PROPERTY), "vm://headersWildcard");
        assertTrue(result.keySet().contains(MuleProperties.MULE_SESSION_PROPERTY));
    }

    public void testMapHeadersMultiWildcard() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersMultiWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        //Will match all Mule headers
        assertEquals(5, result.size());

        //Match on MULE_*
        assertEquals(result.get(MuleProperties.MULE_ENCODING_PROPERTY), "UTF-8");
        assertEquals(result.get(MuleProperties.MULE_ENDPOINT_PROPERTY), "vm://headersMultiWildcard");
        assertTrue(result.keySet().contains(MuleProperties.MULE_SESSION_PROPERTY));

        //Match on ba*
        assertEquals(result.get("bar"), "barValue");
        assertEquals(result.get("baz"), "bazValue");

    }

    public void testMapHeadersWithGenerics() throws Exception
    {
        props.put("apple", new Apple());
        props.put("banana", new Banana());
        props.put("orange", new Orange());

        MuleClient client = new MuleClient(muleContext);
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

    public void testListHeaders() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersList", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(3, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
        assertTrue(result.contains("bazValue"));
    }

    public void testListHeadersWithOptional() throws Exception
    {
        props.remove("baz");
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(2, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
    }

    public void testListHeadersWithMissing() throws Exception
    {
        props.remove("bar");
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof RequiredValueException);
    }

    public void testSingleListHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://singleHeaderList", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        assertEquals(1, result.size());
        assertTrue(result.contains("fooValue"));
    }

    public void testListHeadersUnmodifiable() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListUnmodifiable", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertNotNull("Exception should have been thrown", message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof UnsupportedOperationException);
    }

    public void testListHeadersAll() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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

    public void testMapHeadersListAllOptional() throws Exception
    {
        props.clear();

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListAllOptional", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //We just want ot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    public void testListHeadersWilcard() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //Will match all Mule headers
        assertEquals(3, result.size());
        //MULE_ENCODING
        assertTrue(result.contains("UTF-8"));
        //MULE_ENDPOINT
        assertTrue(result.contains("vm://headersListWildcard"));
        //The last value is the encoded session
    }

    public void testListHeadersMultiWilcard() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://headersListMultiWildcard", null, props);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a List", message.getPayload() instanceof List);
        List<?> result = (List<?>) message.getPayload();
        //Will match on MULE_* and ba*
        assertEquals(5, result.size());

        //Match on MULE_*
        //MULE_ENCODING
        assertTrue(result.contains("UTF-8"));
        //MULE_ENDPOINT
        assertTrue(result.contains("vm://headersListMultiWildcard"));
        //The last value is the encoded session

        //Match on ba*
        //bar
        assertTrue(result.contains("barValue"));

        //baz
        assertTrue(result.contains("bazValue"));
    }

    public void testListHeadersWithGenerics() throws Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        props.put("apple", apple);
        props.put("banana", banana);
        props.put("orange", orange);

        MuleClient client = new MuleClient(muleContext);
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
}
