/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ParseTemplateTransformerTestCase
{
    private static final String EXPRESSION = "#[exp]";

    private ParseTemplateTransformer parseTemplateTransformer;
    private MuleMessage mockMuleMessage;
    private ExpressionManager mockExpressionManager;
    private MuleContext mockMuleContext;

    @Before
    public void setUp()
    {
        parseTemplateTransformer = new ParseTemplateTransformer();
        mockMuleMessage = mock(MuleMessage.class);
        mockMuleContext = mock(MuleContext.class);
        mockExpressionManager = mock(ExpressionManager.class);
        parseTemplateTransformer.setMuleContext(mockMuleContext);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseTemplateNullLocation() throws TransformerException
    {
        parseTemplateTransformer.setLocation(null);
        parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseTemplateNullLocationValue() throws TransformerException, InitialisationException
    {
        parseTemplateTransformer.setLocation(EXPRESSION);
        when(mockExpressionManager.isExpression(EXPRESSION)).thenReturn(true);
        parseTemplateTransformer.initialise();
        when(mockExpressionManager.evaluate(EXPRESSION, mockMuleMessage)).thenReturn(null);
        parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
    }

    @Test
    public void testParseTemplate() throws TransformerException, InitialisationException, IOException
    {
        parseTemplateTransformer.setLocation(EXPRESSION);
        when(mockExpressionManager.isExpression(EXPRESSION)).thenReturn(true);
        parseTemplateTransformer.initialise();
        when(mockExpressionManager.evaluate(EXPRESSION, mockMuleMessage)).thenReturn("error.html");
        when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
        String expectedExpression = IOUtils.getResourceAsString("error.html", this.getClass());
        when(mockExpressionManager.parse(expectedExpression, mockMuleMessage)).thenReturn("Parsed");

        Object response = parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertNotNull(response);
        assertEquals("Parsed", response);
    }


}
