/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.properties;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.sql.Timestamp;

/** TODO */
public class PropertyExtractorManagerTestCase extends AbstractMuleTestCase
{
    public void testManager() throws Exception
    {
        PropertyExtractorManager.setDefaultExtractor(FunctionPropertyExtractor.NAME);

        UMOMessage message = new MuleMessage("test");
        Object o = PropertyExtractorManager.processExpression("uuid", message);
        assertNotNull(o);
        o = PropertyExtractorManager.processExpression("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);
    }

    public void testDefaultExtractor() throws Exception
    {
        assertEquals(MessageHeaderPropertyExtractor.NAME, PropertyExtractorManager.getDefaultExtractor());
        PropertyExtractorManager.setDefaultExtractor(MapPayloadPropertyExtractor.NAME);

        assertEquals(MapPayloadPropertyExtractor.NAME, PropertyExtractorManager.getDefaultExtractor());
        try
        {
            PropertyExtractorManager.setDefaultExtractor("bork");
            fail("bork is not a valid property extractor");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
        assertEquals(MapPayloadPropertyExtractor.NAME, PropertyExtractorManager.getDefaultExtractor());
        //Lets remove the default
        PropertyExtractorManager.unregisterExtractor(MapPayloadPropertyExtractor.NAME);
        assertEquals(PropertyExtractorManager.DEFAULT_EXTRACTOR_NAME, PropertyExtractorManager.getDefaultExtractor());

        try
        {
            PropertyExtractorManager.setDefaultExtractor(MapPayloadPropertyExtractor.NAME);
            fail("Map extractor should no longer be registered");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    public void testRegistration() throws Exception
    {
        try
        {
            PropertyExtractorManager.registerExtractor(new MapPayloadPropertyExtractor());
            fail("extractor already exists");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }

        try
        {
            PropertyExtractorManager.registerExtractor(null);
            fail("null extractor");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
        assertNull(PropertyExtractorManager.unregisterExtractor(null));

    }
}
