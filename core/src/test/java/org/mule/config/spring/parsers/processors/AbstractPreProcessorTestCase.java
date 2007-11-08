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

import org.mule.tck.AbstractMuleTestCase;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.util.ArrayUtils;

import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractPreProcessorTestCase extends AbstractMuleTestCase
{

    protected void assertBad(String[][] constraint, String attributes, String text) throws ParserConfigurationException
    {
        try
        {
            assertOk(constraint, attributes);
            fail("Expected failure with " + attributes + " and " + ArrayUtils.toString(constraint));
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage(), e.getMessage().indexOf(text) > -1);
        }
    }

    protected void assertOk(String[][] constraint, String attributes) throws ParserConfigurationException
    {
        createCheck(constraint).preProcess(null, createElement(attributes));
    }

    protected abstract PreProcessor createCheck(String[][] constraint);

    protected Element createElement(String attributes) throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Element element = document.createElement("element");
        StringTokenizer tokens = new StringTokenizer(attributes);
        while (tokens.hasMoreTokens())
        {
            element.setAttribute(tokens.nextToken(), "value");
        }
        return element;
    }

}