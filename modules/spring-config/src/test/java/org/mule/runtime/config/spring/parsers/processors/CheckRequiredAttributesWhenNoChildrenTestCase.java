/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;

public class CheckRequiredAttributesWhenNoChildrenTestCase extends AbstractPreProcessorTestCase
{

    private static final String MULE_NAMESPACE_URL = "http://www.mulesoft.org/schema/mule/core";

    @Test
    public void testChildWithoutNamespace() throws ParserConfigurationException
    {
        assertOk(new String[][]{new String[] {"constraintAttribute"}}, "", "aChild", null);
    }

    @Test
    public void testChildWithNamespace() throws ParserConfigurationException
    {
        assertOk(new String[][]{new String[] {"constraintAttribute"}}, "", "aChild", MULE_NAMESPACE_URL);
    }

    @Test(expected = CheckRequiredAttributes.CheckRequiredAttributesException.class)
    public void testAttributeNotPresentAndNoChildren() throws CheckRequiredAttributes.CheckRequiredAttributesException, ParserConfigurationException
    {
        assertOk(new String[][]{new String[] {"constraintAttribute"}}, "", null, MULE_NAMESPACE_URL);
    }

    @Test
    public void testAttributePresentAndNoChildren() throws ParserConfigurationException
    {
        assertOk(new String[][]{new String[] {"constraintAttribute"}}, "constraintAttribute", null, MULE_NAMESPACE_URL);
    }

    @Override
    protected PreProcessor createCheck(String[][] constraint, String elementName, String elementNamespaceUrl)
    {
        return new CheckRequiredAttributesWhenNoChildren(constraint, elementName, elementNamespaceUrl);
    }
}
