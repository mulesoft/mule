/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import java.io.IOException;

import org.xml.sax.SAXException;


public class VmSchemaValidationTestCase extends AbstractSchemaValidationTestCase
{

    public void testRootSchema() throws IOException, SAXException
    {
        addSchema("http://www.mulesource.org/schema/mule/vm/2.2", "META-INF/mule-vm.xsd");
        doTest("org/mule/config/spring/vm-validation-test.xml");
    }

}
