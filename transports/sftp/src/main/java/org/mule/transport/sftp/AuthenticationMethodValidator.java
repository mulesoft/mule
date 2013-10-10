/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.JSch;

/**
 *  Validates SFTP authentication methods
 */
public class AuthenticationMethodValidator
{

    private static final String AUTH_PROPERTY_PREFIX = "userauth.";
    private static final String LIST_SEPARATOR = ",";

    private AuthenticationMethodValidator()
    {
    }

    /**
     * Checks that a list contains only valid authentication methods.
     *
     * @param methods comma separated list of authentication methods
     * @throws IllegalArgumentException if there are an invalid authentication method.
     */
    public static void validateAuthenticationMethods(String methods) throws IllegalArgumentException
    {
        String[] split = methods.split(LIST_SEPARATOR);

        for (String method : split)
        {
            String config = JSch.getConfig(AUTH_PROPERTY_PREFIX + method);

            if (config == null)
            {
                throw new IllegalArgumentException("Not a valid authentication method: " + method);
            }
        }
    }
}
