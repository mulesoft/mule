/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BeanPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public BeanPropertyExtractorMultipleEndpointsTestCase()
    {
        super(false);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.property", "${bean:endpointsHolder.endpoints}");
        return p;
    }

    protected Object getMatchMessage()
    {
        //Model a simple bean graph. Path is: endpointsHolder.endpoints
        List endpoints = new ArrayList(2);
        endpoints.add("matchingEndpoint1");
        endpoints.add("matchingEndpoint2");
        return new TestRootBean(new EndpointsHolder(endpoints));
    }

    protected Object getErrorMessage()
    {
        List endpoints = new ArrayList(1);
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
        private List endpoints;

        public EndpointsHolder(List endpoints)
        {
            this.endpoints = endpoints;
        }

        public List getEndpoints()
        {
            return endpoints;
        }

        public void setEndpoints(List endpoints)
        {
            this.endpoints = endpoints;
        }
    }

}