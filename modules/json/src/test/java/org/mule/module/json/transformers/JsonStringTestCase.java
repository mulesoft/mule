/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class JsonStringTestCase extends AbstractMuleContextTestCase
{
    public static final String TEST_JSON_MESSAGE = "{\"data\" : {\"value1\" : \"foo\", \"value2\" : \"bar\"}, \"replyTo\" : \"/response\"}";

    /**
     * Test that a Json string doesn't get modified in any way
     */
    @Test
    public void testTryConvertJsonStringToJsonString() throws Exception
    {
        ObjectToJson transformer = createObject(ObjectToJson.class);
        Object result = transformer.transform(TEST_JSON_MESSAGE);
        assertNotNull(result);
        assertEquals(TEST_JSON_MESSAGE, result);
    }


    @Test
    public void testTryConvertJsonStringToJustString() throws Exception
    {
        ObjectToJson transformer = createObject(ObjectToJson.class);
        //This is still valid json
        assertEquals("\"Hello\"", transformer.transform("Hello"));
    }
}
