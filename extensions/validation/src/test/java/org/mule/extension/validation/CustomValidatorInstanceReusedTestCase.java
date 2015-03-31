/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;

import org.junit.Test;

public class CustomValidatorInstanceReusedTestCase extends ValidationTestCase
{

    private static int HIT_COUNT;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        HIT_COUNT = 0;
    }

    @Override
    protected String getConfigFile()
    {
        return "custom-validator.xml";
    }

    @Test
    public void byClassInstanceReused() throws Exception
    {
        runFlow("byClassInstanceReused");
        assertThat(HIT_COUNT, is(2));
    }

    public static class TestValidator implements Validator
    {

        @Override
        public ValidationResult validate(MuleEvent event)
        {
            HIT_COUNT++;
            return ok();
        }
    }

}
