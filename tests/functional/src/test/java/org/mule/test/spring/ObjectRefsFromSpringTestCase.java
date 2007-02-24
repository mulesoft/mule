/*
 * $Id:ObjectRefsFromSpringTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.providers.SimpleRetryConnectionStrategy;

public class ObjectRefsFromSpringTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/test-refs-from-spring.xml";
    }

    public void testObjectCreation() throws Exception
    {
        UMOEndpoint ep = managementContext.getRegistry().lookupEndpoint("foo");
        assertNotNull(ep);
        assertEquals("testConnector", ep.getConnector().getName());
        assertTrue(ep.getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
        assertTrue(ep.getConnector().getExceptionListener() instanceof TestExceptionStrategy);

        assertNotNull(ep.getTransformer());
        assertEquals("testTransformer", ep.getTransformer().getName());
        assertTrue(ep.getTransformer() instanceof TestCompressionTransformer);
        assertEquals(12, ((TestCompressionTransformer)ep.getTransformer()).getBeanProperty2());

    }
}
