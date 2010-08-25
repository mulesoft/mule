/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.tck.AbstractMuleTestCase;


public class JsonStringTestCase extends AbstractMuleTestCase
{
    public static final String TEST_JSON_MESSAGE = "{\"data\" : {\"value1\" : \"foo\", \"value2\" : \"bar\"}, \"replyTo\" : \"/response\"}";

    /**
     * Test that a Json string doesn't get modified in any way
     */
    public void testTryConvertJsonStringToJsonString() throws Exception
    {
        ObjectToJson transformer = createObject(ObjectToJson.class);
        Object result = transformer.transform(TEST_JSON_MESSAGE);
        assertNotNull(result);
        assertEquals(TEST_JSON_MESSAGE, result);
    }


    public void testTryConvertJsonStringToJustString() throws Exception
    {
        ObjectToJson transformer = createObject(ObjectToJson.class);
        //This is still valid json
        assertEquals("\"Hello\"", transformer.transform("Hello"));
    }
}
