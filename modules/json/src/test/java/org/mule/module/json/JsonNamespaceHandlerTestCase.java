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
import org.mule.module.json.transformers.FruitCollection;
import org.mule.module.json.transformers.JsonBeanRoundTripTestCase;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.module.json.transformers.ObjectToJson;
import org.mule.tck.FunctionalTestCase;

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
        assertTrue(filter.isValidateParsing());

        ObjectToJson serializer = (ObjectToJson) muleContext.getRegistry().lookupObject("fruitCollectionToJson");
        assertNotNull(serializer);
        assertEquals(String.class, serializer.getReturnClass());
        assertEquals(FruitCollection.class, serializer.getSourceClass());
        assertEquals(3, serializer.getSerializationMixins().size());

        JsonToObject deserializer = (JsonToObject) muleContext.getRegistry().lookupObject("jsonToFruitCollection");
        assertNotNull(deserializer);
        assertEquals(FruitCollection.class, deserializer.getReturnClass());
        assertEquals(1, deserializer.getDeserializationMixins().size());

       //Test roundTrip
        FruitCollection fc = JsonBeanRoundTripTestCase.JSON_OBJECT;

        String result = (String)serializer.transform(fc);
        assertNotNull(result);
        assertEquals(JsonBeanRoundTripTestCase.JSON_STRING, result);

        FruitCollection result2 = (FruitCollection)deserializer.transform(result);
        assertNotNull(result2);
        assertEquals(fc, result2);
    }
}
