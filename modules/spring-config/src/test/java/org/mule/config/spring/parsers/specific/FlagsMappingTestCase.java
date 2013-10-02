/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.specific.RegExFilterDefinitionParser.FlagsMapping;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SmallTest
public class FlagsMappingTestCase extends AbstractMuleTestCase
{
    private FlagsMapping flagsMapping;

    @Before
    public void setUp() throws Exception
    {
        flagsMapping = new FlagsMapping();
    }

    @Test
    public void testSetSingleFlagString()
    {
        int result = rewrite("DOTALL");
        assertEquals(Pattern.DOTALL, result);
    }

    @Test
    public void testMultipleFlagsString()
    {
        int result = rewrite("DOTALL,MULTILINE");
        assertEquals(Pattern.DOTALL | Pattern.MULTILINE, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFlagsString()
    {
        flagsMapping.rewrite("WRONG_FLAG");
    }

    private int rewrite(String input)
    {
        Integer result = (Integer) flagsMapping.rewrite(input);
        return result.intValue();
    }
}
