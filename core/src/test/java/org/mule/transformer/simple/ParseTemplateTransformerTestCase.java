/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
    private static final String LOCATION = "error.html";
    private static final String INVALID_LOCATION = "wrong_error.html";

    private ParseTemplateTransformer parseTemplateTransformer;
    private MuleMessage mockMuleMessage = mock(MuleMessage.class);
    private MuleContext mockMuleContext = mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);

    @Before
    public void setUp()
    {
        parseTemplateTransformer = new ParseTemplateTransformer();
        parseTemplateTransformer.setMuleContext(mockMuleContext);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseTemplateNullTemplate() throws TransformerException, InitialisationException
    {
        parseTemplateTransformer.setLocation(LOCATION);
        parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
    }

    @Test(expected = InitialisationException.class)
    public void testParseTemplateNullLocation() throws TransformerException, InitialisationException
    {
        parseTemplateTransformer.setLocation(null);
        parseTemplateTransformer.initialise();
        parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
    }

    @Test(expected = InitialisationException.class)
    public void testParseTemplateInvalidLocation() throws TransformerException, InitialisationException
    {
        parseTemplateTransformer.setLocation(INVALID_LOCATION);
        parseTemplateTransformer.initialise();
        parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
    }

    @Test
    public void testParseTemplate() throws TransformerException, InitialisationException, IOException
    {
        parseTemplateTransformer.setLocation(LOCATION);
        parseTemplateTransformer.initialise();
        when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
        String expectedExpression = IOUtils.getResourceAsString(LOCATION, this.getClass());

        when(mockExpressionManager.parse(expectedExpression, mockMuleMessage)).thenReturn("Parsed");

        Object response = parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertNotNull(response);
        assertEquals("Parsed", response);

        // Call a second time to make sure the template is stored once the transformer has been initialized
        response = parseTemplateTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertNotNull(response);
        assertEquals("Parsed", response);
    }

}
