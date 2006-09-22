/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.test.config;

import org.mule.MuleManager;
import org.mule.config.builders.PlaceholderProcessor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class PlaceholderProcessorTestCase extends AbstractMuleTestCase
{
    public void testSecondPropNameLongerThanValue() throws Exception
    {
        final UMOManager manager = MuleManager.getInstance();
        manager.setProperty("longerPropertyName", "shorterValue");
        manager.setProperty("anotherLongProperty", "anotherValue");
        PlaceholderProcessor processor = new PlaceholderProcessor();
        String result = processor.processValue("${longerPropertyName}/${anotherLongProperty}");
        assertEquals("Wrong property substitution.", "shorterValue/anotherValue", result);
    }

    public void testSecondPropNameShorterThanValue() throws Exception
    {
        final UMOManager manager = MuleManager.getInstance();
        manager.setProperty("longerPropertyName", "shorterValue");
        manager.setProperty("shortProperty", "anotherVeryLongValue");
        PlaceholderProcessor processor = new PlaceholderProcessor();
        String result = processor.processValue("${longerPropertyName}/${shortProperty}");
        assertEquals("Wrong property substitution.", "shorterValue/anotherVeryLongValue", result);
    }

}
