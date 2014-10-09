/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static junit.framework.TestCase.fail;
import org.mule.api.MessagingException;
import org.mule.tck.junit4.FunctionalTestCase;

abstract class AbstractValidateSchemaFunctionalTestCase extends FunctionalTestCase
{

    protected static final String VALIDATE_FLOW = "validate";

    protected void runAndExpectFailure(Object payload) throws Throwable
    {
        try
        {
            runFlow(VALIDATE_FLOW, payload);
            fail("was expecting a failure");
        }
        catch (MessagingException e)
        {
            throw e.getCause();
        }
    }
}
