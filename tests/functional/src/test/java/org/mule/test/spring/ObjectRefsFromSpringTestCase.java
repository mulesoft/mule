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

import org.mule.config.ConfigurationBuilder;
import org.mule.config.ExceptionHelper;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.spring.LegacyXmlException;
import org.mule.tck.AbstractMuleTestCase;

public class ObjectRefsFromSpringTestCase extends AbstractMuleTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/test-refs-from-spring.xml";
    }


    public void testObjectCreation() throws Exception
    {
        try
        {
            ConfigurationBuilder cb = new MuleXmlConfigurationBuilder();
            cb.configure(getConfigResources());
            fail("@ref attributes no longer supported in Mule legacy Xml");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionHelper.getRootException(e) instanceof LegacyXmlException);
            LegacyXmlException ex = (LegacyXmlException) ExceptionHelper.getRootException(e);
            assertEquals(1, ex.getErrors().size());
        }

//        UMOEndpoint ep = managementContext.getRegistry().lookupEndpoint("foo");
//        assertNotNull(ep);
//        assertEquals("testConnector", ep.getConnector().getName());
//        assertTrue(ep.getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
//        assertTrue(ep.getConnector().getExceptionListener() instanceof TestExceptionStrategy);
//
//        assertNotNull(ep.getTransformer());
//        assertEquals("testTransformer", ep.getTransformer().getName());
//        assertTrue(ep.getTransformer() instanceof TestCompressionTransformer);
//        assertEquals(12, ((TestCompressionTransformer) ep.getTransformer()).getBeanProperty2());

    }
}
