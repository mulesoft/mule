/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.mule.tck.size.SmallTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class SchemaValidationTestCase extends AbstractMuleTestCase
{
    private static final String SIMPLE_SCHEMA = "schema1.xsd";

    private static final String INCLUDE_SCHEMA = "schema-with-include.xsd";

    private static final String VALID_XML_FILE = "/validation1.xml";

    private static final String INVALID_XML_FILE = "/validation2.xml";

    @Mock
    private MuleContext muleContext;

    @Test
    public void testValidate() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();

        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(VALID_XML_FILE), muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
    }

    @Test
    public void testDefaultResourceResolverIsPresent() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();

        assertThat(filter.getResourceResolver(), is(not(nullValue())));
    }

    @Test
    public void testValidateWithIncludes() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(INCLUDE_SCHEMA);
        filter.initialise();

        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(VALID_XML_FILE), muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
    }
}
