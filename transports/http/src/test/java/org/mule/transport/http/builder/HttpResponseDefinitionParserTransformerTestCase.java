/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.builder;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class HttpResponseDefinitionParserTransformerTestCase extends AbstractMuleTestCase
{
    private HttpResponseDefinitionParser httpResponseDefinitionParser;

    @Before
    public void setUp()
    {
        httpResponseDefinitionParser = new HttpResponseDefinitionParser("header");
    }

    @Test
    public void testProcessHeaderName()
    {
        Map<String, String> headerNameMapping = populateHeaderNamesMapping();

        for(String headerName : headerNameMapping.keySet())
        {
            String processedHeaderName = httpResponseDefinitionParser.processHeaderName(headerName);
            assertEquals(headerNameMapping.get(headerName), processedHeaderName);
        }
    }

    private Map<String, String> populateHeaderNamesMapping()
    {
        Map<String, String> headerNameMapping = new HashMap<String, String>();
        headerNameMapping.put("cache-control", "Cache-Control");
        headerNameMapping.put("location", "Location");
        headerNameMapping.put("expires", "Expires");
        return headerNameMapping;
    }


}
