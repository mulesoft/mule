/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context;

import org.mule.api.MuleContext;
import org.mule.api.registry.ServiceType;
import org.mule.config.ExceptionHelper;

import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.SpiUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase
{

    public static final String INITIAL_VALUE = "500";
    public static final String VALUE_AFTER_REDEPLOY = "222";
    public static final String TEST_PROTOCOL = "test2";

    @Test
    public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception
    {
        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        String value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value,is(INITIAL_VALUE));
        ctx.dispose();
        URL url = DefaultMuleContextTestCase.class.getClassLoader().getResource(SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()+ "/" + TEST_PROTOCOL + "-exception-mappings.properties");
        File exceptionMappingFile = new File(url.getFile());
        FileWriter fileWriter = null;
        try 
        {
            fileWriter = new FileWriter(exceptionMappingFile);
            fileWriter.append("\njava.lang.IllegalArgumentException=" + VALUE_AFTER_REDEPLOY);
        }
        finally 
        {
            if (fileWriter != null)
            {
                fileWriter.close();
            }
        }
        ctx = new DefaultMuleContextFactory().createMuleContext();
        ctx.setExecutionClassLoader(getClass().getClassLoader());
        value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value, is(VALUE_AFTER_REDEPLOY));
    }

}
