/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BeanPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{
    public BeanPropertyExtractorMultipleEndpointsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, false);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "endpointsHolder.endpoints");
        p.setProperty("selector.evaluator", "bean");
        return p;
    }

    @Override
    protected Object getMatchMessage()
    {
        // Model a simple bean graph. Path is: endpointsHolder.endpoints
        List<String> endpoints = new ArrayList<String>(2);
        endpoints.add("matchingEndpoint1");
        endpoints.add("matchingEndpoint2");
        return new TestRootBean(new EndpointsHolder(endpoints));
    }

    @Override
    protected Object getErrorMessage()
    {
        List<String> endpoints = new ArrayList<String>(1);
        endpoints.add("missingEndpoint");
        return new TestRootBean(new EndpointsHolder(endpoints));
    }

    public class TestRootBean
    {
        private EndpointsHolder endpointsHolder;

        public TestRootBean(EndpointsHolder endpointsHolder)
        {
            this.endpointsHolder = endpointsHolder;
        }

        public EndpointsHolder getEndpointsHolder()
        {
            return endpointsHolder;
        }

        public void setEndpointsHolder(EndpointsHolder endpointsHolder)
        {
            this.endpointsHolder = endpointsHolder;
        }
    }

    public class EndpointsHolder
    {
        private List<String> endpoints;

        public EndpointsHolder(List<String> endpoints)
        {
            this.endpoints = endpoints;
        }

        public List<String> getEndpoints()
        {
            return endpoints;
        }

        public void setEndpoints(List<String> endpoints)
        {
            this.endpoints = endpoints;
        }
    }
}
