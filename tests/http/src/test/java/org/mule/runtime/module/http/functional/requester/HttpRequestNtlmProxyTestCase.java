/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.PROXY_AUTHENTICATE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.PROXY_AUTHORIZATION;

public class HttpRequestNtlmProxyTestCase extends AbstractNtlmTestCase
{

    public HttpRequestNtlmProxyTestCase()
    {
        super(PROXY_AUTHORIZATION, PROXY_AUTHENTICATE, SC_PROXY_AUTHENTICATION_REQUIRED);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-ntlm-proxy-config.xml";
    }

    @Override
    public void validNtlmAuth() throws Exception
    {
        super.validNtlmAuth();
        assertThat(requestUrl, is("http://localhost:9999/basePath/requestPath"));
    }
}
