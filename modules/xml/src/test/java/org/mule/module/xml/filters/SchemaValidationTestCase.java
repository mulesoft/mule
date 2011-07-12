/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
