/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.filters;

import org.mule.DefaultMuleMessage;
import org.mule.tck.AbstractMuleTestCase;


public class SchemaValidationTestCase extends AbstractMuleTestCase
{

    /**
     * tests validation
     */
    public void testValidate() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations("schema1.xsd");
        filter.initialise();
        
        assertTrue(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation1.xml"))));
        assertFalse(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation2.xml"))));
    }

}
