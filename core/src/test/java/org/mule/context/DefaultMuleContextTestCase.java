/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.registry.ServiceType;
import org.mule.config.ExceptionHelper;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.SpiUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase
{

    public static final String INITIAL_VALUE = "500";
    public static final String VALUE_AFTER_REDEPLOY = "222";
    public static final String TEST_PROTOCOL = "test2";

    private SystemExceptionHandler mockSystemExceptionHandler = Mockito.mock(SystemExceptionHandler.class);
    private MessagingException mockMessagingException = Mockito.mock(MessagingException.class);

    @Test
    public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception
    {
        URL baseUrl = DefaultMuleContextTestCase.class.getClassLoader().getResource(".");
        File file = new File(baseUrl.getFile() + SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()+ "/" + TEST_PROTOCOL + "-exception-mappings.properties");
        createExceptionMappingFile(file, INITIAL_VALUE);

        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        String value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value,is(INITIAL_VALUE));
        ctx.dispose();

        createExceptionMappingFile(file, VALUE_AFTER_REDEPLOY);

        ctx = new DefaultMuleContextFactory().createMuleContext();
        ctx.setExecutionClassLoader(getClass().getClassLoader());
        value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value, is(VALUE_AFTER_REDEPLOY));
    }

    private void createExceptionMappingFile(File exceptionMappingFile, String value) throws IOException
    {
        FileWriter fileWriter = null;
        try
        {
            fileWriter = new FileWriter(exceptionMappingFile);
            fileWriter.append("\njava.lang.IllegalArgumentException=" + value);
        }
        finally
        {
            if (fileWriter != null)
            {
                fileWriter.close();
            }
        }
    }

    @Test
    public void callSystemExceptionHandlerWhenExceptionIsMessagingException() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        context.setExceptionListener(mockSystemExceptionHandler);
        context.handleException(mockMessagingException);
        verify(mockSystemExceptionHandler, VerificationModeFactory.times(1)).handleException(mockMessagingException,null);
    }

}
