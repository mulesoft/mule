/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
