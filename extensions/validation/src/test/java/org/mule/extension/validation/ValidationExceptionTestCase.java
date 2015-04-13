/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.api.MuleEvent;
import org.mule.extension.validation.api.ExceptionFactory;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

public class ValidationExceptionTestCase extends ValidationTestCase
{

    private static final Exception CUSTOM_EXCEPTION = new ValidationException(error("failed"), mock(MuleEvent.class));

    @Override
    protected String getConfigFile()
    {
        return "validation-exception.xml";
    }

    @Test
    public void byRefExceptionFactory() throws Exception
    {
        assertCustomExceptionFactory("byRefExceptionFactoryFlow");
    }

    @Test
    public void byClassExceptionFactory() throws Exception
    {
        assertCustomExceptionFactory("byClassExceptionFactoryFlow");
    }

    @Test
    public void globalExceptionFactory() throws Exception
    {
        assertCustomExceptionFactory("globalExceptionFactoryFlow");
    }

    @Test
    public void customMessage() throws Exception
    {
        try
        {
            runFlow("customMessage");
            fail("was expecting failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(cause.getMessage(), is("Hello World!"));
        }
    }

    @Test
    public void customExceptionType() throws Exception
    {
        try
        {
            runFlow("customExceptionType");
            fail("was expecting failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
        }
    }

    private void assertCustomExceptionFactory(String flowName) throws Exception
    {
        try
        {
            runFlow(flowName);
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(CUSTOM_EXCEPTION, is(sameInstance(cause)));
        }
    }

    public static class TestExceptionFactory implements ExceptionFactory
    {

        @Override
        public <T extends Exception> T createException(ValidationResult result, Class<T> exceptionClass, MuleEvent event)
        {
            return (T) CUSTOM_EXCEPTION;
        }

        @Override
        public Exception createException(ValidationResult result, String exceptionClassName, MuleEvent event)
        {
            return CUSTOM_EXCEPTION;
        }
    }
}
