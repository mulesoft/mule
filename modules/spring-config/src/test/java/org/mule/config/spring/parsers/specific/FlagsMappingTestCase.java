/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
