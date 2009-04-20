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

import org.mule.module.json.filters.IsJsonFilter;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.module.json.transformers.JsonToXml;
import org.mule.module.json.transformers.ObjectToJson;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

public class JsonNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "json-namespace-config.xml";
    }

    public void testJsonConfig() throws Exception
    {
        IsJsonFilter filter = (IsJsonFilter) muleContext.getRegistry().lookupObject("jsonFilter");
        assertNotNull(filter);

        JsonToObject jsonToOrangeArray = (JsonToObject) muleContext.getRegistry().lookupObject("jsonToOrangeArray");
        assertNotNull(jsonToOrangeArray);
        assertEquals(Orange.class, jsonToOrangeArray.getReturnClass());
        assertNotNull(jsonToOrangeArray.getJsonConfig());
        assertNotNull(jsonToOrangeArray.getJsonConfig().getClassMap());
        assertEquals(Apple.class, jsonToOrangeArray.getJsonConfig().getClassMap().get("apple"));
        assertEquals(Banana.class, jsonToOrangeArray.getJsonConfig().getClassMap().get("banana"));

        ObjectToJson orangeArrayToJson = (ObjectToJson) muleContext.getRegistry().lookupObject("orangeToJson");
        assertNotNull(orangeArrayToJson);
        assertEquals(String.class, orangeArrayToJson.getReturnClass());
        assertEquals(Orange.class, orangeArrayToJson.getSourceClass());
        assertEquals("brand, radius", orangeArrayToJson.getExcludeProperties());

        JsonToXml jsonToXml = (JsonToXml) muleContext.getRegistry().lookupObject("jsonToXml");
        assertNotNull(jsonToXml);
        assertEquals(String.class, jsonToXml.getReturnClass());
        assertEquals("obj", jsonToXml.getObjectElementName());
        assertEquals("array", jsonToXml.getArrayElementName());
        assertEquals("value", jsonToXml.getValueElementName());

    }
}
