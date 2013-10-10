/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
