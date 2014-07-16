/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
