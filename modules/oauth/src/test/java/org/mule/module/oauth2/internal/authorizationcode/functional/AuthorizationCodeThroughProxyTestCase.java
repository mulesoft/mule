/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static org.mule.tck.functional.FlowAssert.verify;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeThroughProxyTestCase extends AbstractAuthorizationCodeFullConfigTestCase
{

    @Rule
    public SystemProperty proxyCount = new SystemProperty("proxyCount", "1");

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Override
    protected String[] getConfigFiles()
    {
        return new String [] {"proxy/oauth-proxy-template.xml", "authorization-code/authorization-code-through-proxy.xml"};
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        super.hitRedirectUrlAndGetToken();
        verify("proxyTemplate");
    }

}
