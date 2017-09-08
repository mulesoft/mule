/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.module.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRequestNtlmAuthTestCase extends AbstractNtlmTestCase
{

    @Parameterized.Parameter(0)
    public String flowName;

    @Parameterized.Parameter(1)
    public String domain;

    @Parameterized.Parameter(2)
    public String workstation;

    @Before
    public void setup()
    {
        setupTestAuthorizer(AUTHORIZATION, WWW_AUTHENTICATE, SC_UNAUTHORIZED);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"ntlmAuthRequestWithDomain", "Ursa-Minor", null},
                {"ntlmAuthRequestWithoutDomain", "", null},
                {"ntlmAuthRequestWithWorkstation", "Ursa-Minor", "LightCity"}});
    }

    @Override
    protected String getWorkstation()
    {
        return workstation;
    }

    @Override
    protected String getDomain()
    {
        return domain;
    }

    @Override
    protected String getFlowName()
    {
        return flowName;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-ntlm-auth-config.xml";
    }


}
