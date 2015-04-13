/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

public class LocalizedValidationMessageTestCase extends ValidationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "validation-i18n.xml";
    }

    @Test
    public void localizedMessage() throws Exception
    {
        try
        {
            runFlow("localizedMessage");
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(cause.getMessage(), is("Se esperaba que el valor fuera true pero fue false"));
        }
    }

}
