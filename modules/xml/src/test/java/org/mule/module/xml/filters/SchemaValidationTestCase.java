/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.filters;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class SchemaValidationTestCase extends AbstractMuleTestCase
{
    MuleContext muleContext = mock(MuleContext.class);

    /**
     * tests validation
     */
    @Test
    public void testValidate() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations("schema1.xsd");
        filter.initialise();
        
        assertTrue(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation1.xml"), muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation2.xml"), muleContext)));
    }

}
