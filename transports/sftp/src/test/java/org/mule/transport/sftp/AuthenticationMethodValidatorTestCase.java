/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class AuthenticationMethodValidatorTestCase extends AbstractMuleTestCase
{

    @Test
    public void acceptsPasswordAuthentication() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("password");
    }

    @Test
    public void acceptsKeyboardInteractiveAuthentication() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("keyboard-interactive");
    }

    @Test
    public void acceptsPublicKeyAuthentication() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("publickey");
    }

    @Test
    public void acceptsGssapiWithMicAuthentication() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("gssapi-with-mic");
    }

    @Test
    public void acceptsMultipleAuthentications() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("password,publickey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidAuthentication() throws Exception
    {
        AuthenticationMethodValidator.validateAuthenticationMethods("invalid-auth");
    }
}
