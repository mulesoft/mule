/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.module.json.filters.IsJsonFilter;
import org.mule.module.json.transformers.FruitCollection;
import org.mule.module.json.transformers.JsonBeanRoundTripTestCase;
import org.mule.module.json.transformers.JsonSchemaValidationFilter;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.module.json.transformers.JsonToXml;
import org.mule.module.json.transformers.JsonXsltTransformer;
import org.mule.module.json.transformers.ObjectToJson;
import org.mule.module.json.transformers.XmlToJson;
import org.mule.tck.junit4.FunctionalTestCase;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

public class JsonNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "json-namespace-config.xml";
    }

    @Test
    public void testJsonConfig() throws Exception
    {
        JsonToXml jToX = muleContext.getRegistry().lookupObject("jToX");
        assertNotNull(jToX);

        XmlToJson xToJ = muleContext.getRegistry().lookupObject("xToJ");
        assertNotNull(xToJ);

        JsonXsltTransformer jToJ = muleContext.getRegistry().lookupObject("jToJ");
        assertNotNull(jToJ);
        assertNotNull(jToJ.getXslt());

        JsonSchemaValidationFilter jvf = muleContext.getRegistry().lookupObject("jvf");
        assertNotNull(jvf);
        assertNotNull(jvf.getSchemaLocations());

        // This test fails under Java 1.6 on Windows, because the Java fields are serialized in a different order.
        String javaVersion = System.getProperty("java.specification.version", "<None>");
        String osName = System.getProperty("os.name", "<None>");
        if (javaVersion.equals("1.6") && osName.startsWith("Windows"))
        {
            return;
        }
          
        IsJsonFilter filter = (IsJsonFilter) muleContext.getRegistry().lookupObject("jsonFilter");
        assertNotNull(filter);
        assertTrue(filter.isValidateParsing());

        ObjectToJson serializer = (ObjectToJson) muleContext.getRegistry().lookupObject("fruitCollectionToJson");
        serializer.initialise();
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
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(result);
        JsonNode expectedJsonNode = mapper.readTree(JsonBeanRoundTripTestCase.JSON_STRING);
        assertEquals(actualJsonNode, expectedJsonNode);

        FruitCollection result2 = (FruitCollection)deserializer.transform(result);
        assertNotNull(result2);
        assertEquals(fc, result2);
    }

}
