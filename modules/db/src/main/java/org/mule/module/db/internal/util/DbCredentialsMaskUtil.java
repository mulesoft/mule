/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.util;

import static java.util.regex.Pattern.compile;
import static org.mule.api.util.CredentialsMaskUtil.PASSWORD_MASK;
import static org.mule.api.util.CredentialsMaskUtil.maskUrlPattern;

import java.util.regex.Pattern;

/**
 * Utils to mask credentials in connection strings
 */
public final class DbCredentialsMaskUtil
{

    private static final Pattern CREDENTIALS_PATTERN_PREFIX = compile(":([^\\s@]+)");

    private DbCredentialsMaskUtil()
    {

    }

    /**
     * Masks credentials in connectionToString prefixed
     * 
     * @param connectionString credentials for user and password to be masked
     * @return connectionString with user and password masked
     */
    public static String maskUrlCredentialsPrefixed(String connectionString)
    {
        return maskUrlPattern(connectionString, CREDENTIALS_PATTERN_PREFIX, PASSWORD_MASK);
    }

}
