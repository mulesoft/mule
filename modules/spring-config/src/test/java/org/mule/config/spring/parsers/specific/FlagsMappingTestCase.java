/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.specific.RegExFilterDefinitionParser.FlagsMapping;
import org.mule.tck.AbstractMuleTestCase;

import java.util.regex.Pattern;

public class FlagsMappingTestCase extends AbstractMuleTestCase
{
    private FlagsMapping flagsMapping;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        flagsMapping = new FlagsMapping();
    }

    public void testSetSingleFlagString()
    {
        int result = rewrite("DOTALL");
        assertEquals(Pattern.DOTALL, result);
    }

    public void testMultipleFlagsString()
    {
        int result = rewrite("DOTALL,MULTILINE");
        assertEquals(Pattern.DOTALL | Pattern.MULTILINE, result);
    }

    public void testInvalidFlagsString()
    {
        try
        {
            flagsMapping.rewrite("WRONG_FLAG");
            fail();
        }
        catch (IllegalArgumentException iae)
        {
            // this one was expected
        }
    }

    private int rewrite(String input)
    {
        Integer result = (Integer) flagsMapping.rewrite(input);
        return result.intValue();
    }
}
