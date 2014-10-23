/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import org.junit.Test;

public class ImportedWsdlTypesFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "imported-wsdl-types-config.xml";
    }

    // This test verifies that types defined in an imported WSDL are processed correctly. The type of the input
    // message is imported from another WSDL. If this import isn't processed correctly, WS consumer assumes that the WS
    // has no parameters and ignores the payload (which generates an invalid response).
    @Test
    public void resolvesTypesFromImportedWsdl() throws Exception
    {
        assertValidResponse("vm://in");
    }
}
