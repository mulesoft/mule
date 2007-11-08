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

public class CheckRequiredAttributesTestCase extends AbstractPreProcessorTestCase
{

    public void testTwoSets() throws ParserConfigurationException
    {
        String[][] a1b2 = new String[][]{new String[]{"a1"}, new String[]{"b1", "b2"}};
        String text12 = "must have all attributes for one of the sets: a1; b1, b2";
        assertBad(a1b2, "", text12);
        assertBad(a1b2, "x", text12);
        assertBad(a1b2, "b2", text12);
        assertBad(a1b2, "x b1", text12);
        assertOk(a1b2, "a1");
        assertOk(a1b2, "a1 x");
        assertOk(a1b2, "b1 b2");
        assertOk(a1b2, "a1 b1");
        assertOk(a1b2, "a1 b2");
        assertOk(a1b2, "a1 b1 b2");
        assertOk(a1b2, "a1 b2 x");
        String[][] a1b0 = new String[][]{new String[]{"a1"}, new String[]{}};
        String text10 = "must have all attributes for one of the sets: a1";
        assertBad(a1b0, "", text10);
        assertBad(a1b0, "x", text10);
        assertBad(a1b0, "b2", text10);
        assertBad(a1b0, "x b1", text10);
        assertOk(a1b0, "a1");
        assertOk(a1b0, "a1 x");
        assertBad(a1b0, "b1 b2", text10);
        assertOk(a1b0, "a1 b1");
        assertOk(a1b0, "a1 b2");
        assertOk(a1b0, "a1 b1 b2");
        assertOk(a1b0, "a1 b2 x");
    }

    public void testSingleSet() throws ParserConfigurationException
    {
        String[][] a1 = new String[][]{new String[]{"a1"}};
        String text1 = "must have all attributes for one of the sets: a1";
        assertBad(a1, "", text1);
        assertBad(a1, "x", text1);
        assertBad(a1, "b2", text1);
        assertBad(a1, "x b1", text1);
        assertOk(a1, "a1");
        assertOk(a1, "a1 x");
        assertBad(a1, "b1 b2", text1);
        assertOk(a1, "a1 b1");
        assertOk(a1, "a1 b2");
        assertOk(a1, "a1 b1 b2");
        assertOk(a1, "a1 b2 x");
        String[][] b2 = new String[][]{new String[]{"b1", "b2"}};
        String text2 = "must have all attributes for one of the sets: b1, b2";
        assertBad(b2, "", text2);
        assertBad(b2, "x", text2);
        assertBad(b2, "b2", text2);
        assertBad(b2, "x b1", text2);
        assertBad(b2, "a1", text2);
        assertBad(b2, "a1 x", text2);
        assertOk(b2, "b1 b2");
        assertBad(b2, "a1 b1", text2);
        assertBad(b2, "a1 b2", text2);
        assertOk(b2, "a1 b1 b2");
        assertBad(b2, "a1 b2 x", text2);
    }

    protected PreProcessor createCheck(String[][] constraint)
    {
        return new CheckRequiredAttributes(constraint);
    }

}
