/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.tck.junit4.DomainFunctionalTestCase;

import org.junit.Test;

public class HttpSharedRequestConfigTestCase extends DomainFunctionalTestCase
{

    private static final String FIRST_APP_NAME = "app-1";
    private static final String SECOND_APP_NAME = "app-2";

    @Override
    protected String getDomainConfig()
    {
        return "domain/http/http-shared-request-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[]{
                new ApplicationConfig(FIRST_APP_NAME, new String[] {"domain/http/http-request-app.xml"}),
                new ApplicationConfig(SECOND_APP_NAME, new String[] {"domain/http/http-request-app.xml"})
        };
    }

    @Test
    public void useSameRequestConfig() throws Exception
    {
        final DefaultHttpRequester firstAppRequester = getMuleContextForApp(FIRST_APP_NAME).getRegistry().lookupObject(DefaultHttpRequester.class);
        final DefaultHttpRequester secondAppRequester = getMuleContextForApp(FIRST_APP_NAME).getRegistry().lookupObject(DefaultHttpRequester.class);
        assertThat(firstAppRequester.getConfig(), is(secondAppRequester.getConfig()));
    }

}
