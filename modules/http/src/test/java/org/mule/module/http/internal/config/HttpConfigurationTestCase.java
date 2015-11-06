/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import org.mule.api.MuleContext;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class HttpConfigurationTestCase extends AbstractMuleTestCase
{

    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
    private HttpConfiguration httpConfiguration = new HttpConfiguration();

    @Test
    public void defaultIsUseNewModule()
    {
        when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(null);
        assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(false));
    }

    @Test
    public void noSystemPropertyAndNoConfig() throws Exception
    {
        when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(null);
        assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(false));
    }


    @Test
    public void systemPropertyWithNoConfig() throws Exception
    {
        testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, TRUE.toString(),
           new MuleTestUtils.TestCallback()
           {
               @Override
               public void run() throws Exception
               {
                   when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(null);
                   assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(true));
               }
           });

    }

    @Test
    public void systemPropertyTrueButConfigWithFalse() throws Exception
    {
        testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, TRUE.toString(),
               new MuleTestUtils.TestCallback()
               {
                   @Override
                   public void run() throws Exception
                   {
                       when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(httpConfiguration);
                       httpConfiguration.setUseTransportForUris(false);
                       assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(false));
                   }
               });

    }

    @Test
    public void systemPropertyFalseAndConfigWithFalse() throws Exception
    {
        testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, FALSE.toString(),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(httpConfiguration);
                                       httpConfiguration.setUseTransportForUris(false);
                                       assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(false));
                                   }
                               });

    }

    @Test
    public void systemPropertyFalseButConfigWithTrue() throws Exception
    {
        testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, FALSE.toString(),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       when((Object) (mockMuleContext.getConfiguration().getExtension(HttpConfiguration.class))).thenReturn(httpConfiguration);
                                       httpConfiguration.setUseTransportForUris(true);
                                       assertThat(HttpConfiguration.useTransportForUris(mockMuleContext), is(true));
                                   }
                               });

    }

}
