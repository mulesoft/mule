/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.validation.internal.ValidationExtension;
import org.mule.extension.validation.api.ValidationException;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

public class ValidateSchemaWithValidationExtensionTestCase extends ExtensionsFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {ValidationExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "validate-schema-with-validation-module-config.xml";
    }

    @Test
    public void validateInGroup() throws Exception
    {
        try
        {
            runFlow("validate");
            fail("Was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(cause, is(instanceOf(ValidationException.class)));
        }
    }
}
