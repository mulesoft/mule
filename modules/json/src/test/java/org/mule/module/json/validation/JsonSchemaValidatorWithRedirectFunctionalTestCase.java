/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstab;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstab2;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getGoodFstab;

import org.junit.Test;

public class JsonSchemaValidatorWithRedirectFunctionalTestCase extends AbstractValidateSchemaFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "validate-schema-with-redirects-config.xml";
    }

    @Test
    public void good() throws Exception
    {
        runFlow(VALIDATE_FLOW, getGoodFstab());
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad() throws Throwable
    {
        runAndExpectFailure(getBadFstab());
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad2() throws Throwable
    {
        runAndExpectFailure(getBadFstab2());
    }
}
