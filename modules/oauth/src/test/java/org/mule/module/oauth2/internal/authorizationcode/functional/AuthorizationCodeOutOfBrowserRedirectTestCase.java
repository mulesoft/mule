/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

public class AuthorizationCodeOutOfBrowserRedirectTestCase extends AbstractAuthorizationCodeBasicTestCase
{

    @Override
    protected String getRedirectUrl()
    {
        return "urn:ietf:wg:oauth:2.0:oob";
    }
    
    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-minimal-config.xml";
    }

}
