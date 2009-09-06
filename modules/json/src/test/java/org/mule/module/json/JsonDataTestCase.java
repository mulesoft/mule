/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.module.json.transformers.JsonToObject;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

public class JsonDataTestCase extends AbstractMuleTestCase
{
    public void testReadingArrayData() throws Exception
    {
        String json = IOUtils.getResourceAsString("test-data.json", getClass());
        JsonToObject trans  = createObject(JsonToObject.class);
        trans.setReturnClass(JsonData.class);
        JsonData jsonData = (JsonData)trans.transform(json);
        assertTrue(jsonData.isArray());
        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492", jsonData.get("[0]->text"));

        assertEquals("Mule Test", jsonData.get("[0]->user->name"));
        assertEquals("Mule Test9", jsonData.get("[9]->user->name"));
        // test toString() since it was broken for arrays
        assertNotNull(jsonData.toString());
        try
        {
            jsonData.get("[0]->user->XXX");
            fail("Property XXX does not exist");
        }
        catch (Exception e)
        {
            //expected
        }
        try
        {
            jsonData.get("foo[0]->user");
            fail("foo is not the root element name");
        }
        catch (Exception e)
        {
            //expected
        }

        try
        {
            jsonData.get("[10]->user");
            fail("Index should be out of bounds");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    public void testReadingComplexData() throws Exception
    {
        String json = IOUtils.getResourceAsString("filters.json", getClass());
        JsonToObject trans  = createObject(JsonToObject.class);
        trans.setReturnClass(JsonData.class);
        JsonData jsonData = (JsonData)trans.transform(json);
        assertFalse(jsonData.isArray());

        assertEquals("/**", jsonData.get("filters[0]->channels"));
        assertEquals("teh ", jsonData.get("filters[1]->init[1][0]"));        
        assertEquals("the ", jsonData.get("filters[1]->init[1][1]"));
        // test toString() since it was broken for arrays
        assertNotNull(jsonData.toString());
    }
}
