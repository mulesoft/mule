/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueExtractorTransformerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testDoesNotFailIfNoExpressionMatchesWithNoDefaultValue() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TESTw+TEST", "#[header:INVOCATION:propName]", false, null));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TEST", enrichExpressionPairs);
        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        Object response = transformer.transform(testMessage);

        DefaultMuleMessage responseMessage = (DefaultMuleMessage) response;
        Set<String> propertyNames = responseMessage.getPropertyNames(PropertyScope.INVOCATION);
        assertEquals("Should not match any property", 0, propertyNames.size());
    }

    @Test
    public void testUsesDefaultIfNoExpressionMatches() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TESTw+TEST", "#[header:INVOCATION:propName]", false, "foo"));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TEST", enrichExpressionPairs);
        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        Object response = transformer.transform(testMessage);
        DefaultMuleMessage responseMessage = (DefaultMuleMessage) response;
        assertPropertyAddedToMessage(responseMessage, "propName", "foo", PropertyScope.INVOCATION);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailIfNoExpressionMatchesAndNoDefaultValueSpecified() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TESTw+TEST", "#[header:INVOCATION:propName]", true, null));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TEST", enrichExpressionPairs);
        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        transformer.transform(testMessage);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailsIfExpressionMatchesMultipleValues() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TEST(\\w+)TEST(\\w+)", "#[header:INVOCATION:propName]", true, null));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TESTfooTESTbar", enrichExpressionPairs);
        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        transformer.transform(testMessage);
    }

    @Test
    public void testAddsSingleValue() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TEST(\\w+)TEST", "#[header:INVOCATION:propName]", false, null));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TESTfooTEST", enrichExpressionPairs);

        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        Object response = transformer.transform(testMessage);
        DefaultMuleMessage responseMessage = (DefaultMuleMessage) response;
        assertPropertyAddedToMessage(responseMessage, "propName", "foo", PropertyScope.INVOCATION);
    }

    @Test
    public void testAddsMultipleValues() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TEST(\\w+)TEST\\w+TEST", "#[header:INVOCATION:propName1]", false, null));
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TEST\\w+TEST(\\w+)TEST", "#[header:INVOCATION:propName2]", false, null));
        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("TESTfooTESTbarTEST", enrichExpressionPairs);

        DefaultMuleMessage testMessage = new DefaultMuleMessage("TEST", muleContext);

        Object response = transformer.transform(testMessage);

        DefaultMuleMessage responseMessage = (DefaultMuleMessage) response;
        assertPropertyAddedToMessage(responseMessage, "propName1", "foo", PropertyScope.INVOCATION);
        assertPropertyAddedToMessage(responseMessage, "propName2", "bar", PropertyScope.INVOCATION);
    }

    @Test
    public void testAddsSingleValueUsingSourceExpression() throws Exception
    {
        List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();
        enrichExpressionPairs.add(new ValueExtractorTransformer.ValueExtractorTemplate("TEST(\\w+)TEST", "#[header:INVOCATION:propName]", false, null));

        ValueExtractorTransformer transformer = createPropertyGeneratorTransformer("#[payload:]", enrichExpressionPairs);

        DefaultMuleMessage testMessage = new DefaultMuleMessage("TESTfooTEST", muleContext);

        Object response = transformer.transform(testMessage);
        DefaultMuleMessage responseMessage = (DefaultMuleMessage) response;

        assertPropertyAddedToMessage(responseMessage, "propName", "foo", PropertyScope.INVOCATION);
    }

    private void assertPropertyAddedToMessage(MuleMessage message, String propertyName, String value, PropertyScope scope)
    {
        Set<String> propertyNames = message.getPropertyNames(scope);
        assertTrue(String.format("Property '%s' was not added to the message", propertyName), propertyNames.contains(propertyName));
        assertEquals(value, message.getProperty(propertyName, scope));
    }

    private ValueExtractorTransformer createPropertyGeneratorTransformer(String source, List<ValueExtractorTransformer.ValueExtractorTemplate> enrichExpressionPairs) throws Exception
    {
        ValueExtractorTransformer transformer = new ValueExtractorTransformer();
        transformer.setSource(source);
        transformer.setValueExtractorTemplates(enrichExpressionPairs);
        transformer.setMuleContext(muleContext);

        return transformer;
    }

}
