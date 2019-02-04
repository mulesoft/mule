/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

public class JsonDataTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testReadingArrayData() throws Exception
    {
        JsonData jsonData = readJsonData("test-data.json");
        assertTrue(jsonData.isArray());
        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492", jsonData.getAsString("[0]/text"));
        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492",
            jsonData.getAsString("[0]/'text'"));

        assertEquals("Mule Test", jsonData.getAsString("[0]/'user'/name"));
        assertEquals("Mule Test9", jsonData.getAsString("[9]/user/name"));
        // test toString() since it was broken for arrays
        assertNotNull(jsonData.toString());
        try
        {
            assertNull(jsonData.get("[0]/user/XXX"));
            fail("Property XXX does not exist");
        }
        catch (Exception e)
        {
            // expected
        }
        try
        {
            jsonData.get("foo[0]/user");
            fail("foo is not the root element name");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            jsonData.get("[10]/user");
            fail("Index should be out of bounds");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testReadingComplexData() throws Exception
    {
        JsonData jsonData = readJsonData("filters.json");
        assertFalse(jsonData.isArray());

        // assertEquals("/**", jsonData.get("filters[0]/channels"));
        assertEquals("teh ", jsonData.getAsString("filters[1]/init[1][0]"));
        assertEquals("the ", jsonData.getAsString("filters[1]/init[1][1]"));
        // test toString() since it was broken for arrays
        assertNotNull(jsonData.toString());
    }

    @Test
    public void testReadingWithQuotedString() throws Exception
    {
        JsonData jsonData = readJsonData("bitly-response.json");
        assertEquals("NfeyS",
            jsonData.getAsString("results/'http://rossmason.blogspot.com/2008/01/about-me.html'/hash"));
    }

    @Test
    public void testReadingArray() throws Exception
    {
        JsonData jsonData = readJsonData("flickr-response.json");

        assertEquals("4136507840", jsonData.getAsString("photos/photo[0]/id"));

        ArrayNode photos = (ArrayNode) jsonData.get("photos/photo");
        assertNotNull(photos);
        assertEquals(10, photos.size());

        Object o = jsonData.get("photos");
        assertNotNull(o);
        assertTrue(o instanceof ObjectNode);
    }

    @Test
    public void testReadingANumber() throws Exception
    {
        JsonParser parser = new DefaultJsonParser(muleContext);
        String jsonAsString = "{\"value\":210.0}";

        JsonNode json = parser.asJsonNode(jsonAsString);
        assertThat("JSON has not changed", jsonAsString.equals(json.toString()));
    }

    private JsonData readJsonData(String filename) throws Exception
    {
        String json = IOUtils.getResourceAsString(filename, getClass());
        return new JsonData(json);
    }
}
