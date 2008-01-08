/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;

import javax.xml.parsers.ParserConfigurationException;

public class CheckExclusiveAttributesTestCase extends AbstractPreProcessorTestCase
{

    public void testAttributes() throws ParserConfigurationException
    {
        String[][] a1b2 = new String[][]{new String[]{"a1"}, new String[]{"b1", "b2"}};
        String text = "cannot appear with the attribute";
        assertOk(a1b2, "");
        assertOk(a1b2, "x");
        assertOk(a1b2, "b2");
        assertOk(a1b2, "x b1");
        assertOk(a1b2, "a1");
        assertOk(a1b2, "a1 x");
        assertOk(a1b2, "b1 b2");
        assertBad(a1b2, "a1 b1", text);
        assertBad(a1b2, "a1 b2", text);
        assertBad(a1b2, "a1 b1 b2", text);
        assertBad(a1b2, "a1 b2 x", text); 
        String[][] a1b0 = new String[][]{new String[]{"a1"}, new String[]{}};
        assertOk(a1b0, "");
        assertOk(a1b0, "x");
        assertOk(a1b0, "b2");
        assertOk(a1b0, "x b1");
        assertOk(a1b0, "a1");
        assertOk(a1b0, "a1 x");
        assertOk(a1b0, "b1 b2");
        assertOk(a1b0, "a1 b1");
        assertOk(a1b0, "a1 b2");
        assertOk(a1b0, "a1 b1 b2");
        assertOk(a1b0, "a1 b2 x"); 
    }

    protected PreProcessor createCheck(String[][] constraint)
    {
        return new CheckExclusiveAttributes(constraint);
    }

}