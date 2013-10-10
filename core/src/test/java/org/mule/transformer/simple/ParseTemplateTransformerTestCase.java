/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
