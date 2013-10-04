/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;


public class RootSchemaValidationTestCase extends AbstractSchemaValidationTestCase
{

    @Test
    public void testRootSchema() throws IOException, SAXException
    {
        doTest("org/mule/config/spring/root-validation-test.xml");
    }

}
