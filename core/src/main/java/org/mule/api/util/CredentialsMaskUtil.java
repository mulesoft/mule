/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.util;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CredentialsMaskUtil
{
    public static final Pattern BARE_URL_PASSWORD_PATTERN = compile("[a-z]*://[a-zA-Z0-9%._-]*:([^@]*)@");
    public static final Pattern BARE_URL_PATTERN = compile("[a-z]*://([^@]*)@");
    public static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
    public static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
    public static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"|\n|>]*)\"");
    public static final Pattern PASSWORD_PATTERN_NO_QUOTES = compile("password=([^\\s;]+)");
    public static final Pattern USER_PATTERN_NO_QUOTES = compile("user=([^\\s;]+)");
    public static final String PASSWORD_MASK = "<<credentials>>";
    public static final String USER_MASK = "<<user>>";
    public static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

    private static final String USER_URL_PREFIX = "user=";
    private static final String PASSWORD_URL_PREFIX = "password=";
    
    /**
     * masks url credentials
     * 
     * @param input input for credentials to be masked
     * 
     * @return input with password masked
     */
    public static String maskPasswords(String input)
    {
        input = maskUrlPassword(input, URL_PATTERN);
        input = maskUrlPassword(input, ADDRESS_PATTERN);

        Matcher matcher = PASSWORD_PATTERN.matcher(input);
        if (matcher.find() && matcher.groupCount() > 0)
        {
            input = input.replace(maskPasswordAttribute(matcher.group(1)), maskPasswordAttribute(PASSWORD_MASK));
        }

        return input;
    }

    /**
     * masks password in input
     * 
     * @param input input for password to be masked
     * @param pattern password pattern
     * 
     * @return input with password masked
     */
    public static String maskUrlPassword(String input, Pattern pattern)
    {
        return maskUrlPattern(input, pattern, PASSWORD_MASK);
    }

    /**
     * masks password in input
     *
     * @param input input for password to be masked
     * @param pattern password pattern
     * @param mask string to mask the password with
     *
     * @return input with password masked
     */
    public static String maskUrlPasswordWithMask(String input, Pattern pattern, String mask)
    {
        return maskUrlPattern(input, pattern, mask);
    }

    /**
     * masks user and password in input
     * 
     * @param input input for user and password to be masked
     * @param passwordPattern password pattern
     * @param userPattern user pattern
     * 
     * @return input with user and password masked
     */
    public static String maskUrlUserAndPassword(String input, Pattern passwordPattern, Pattern userPattern)
    {
        String inputMasked = maskUrlPattern(input, passwordPattern, PASSWORD_MASK, PASSWORD_URL_PREFIX);
        return maskUrlPattern(inputMasked, userPattern, USER_MASK, USER_URL_PREFIX);
    }

    /**
     * masks password attribute
     * 
     * @param password password to be masked
     * 
     * @return password masked
     */
    public static String maskPasswordAttribute(String password)
    {
        return format(PASSWORD_ATTRIBUTE_MASK, password);
    }

    private static String maskUrlPattern(String input, Pattern pattern, String mask) {
        return maskUrlPattern(input, pattern, mask, "");
    }
    
    private static String maskUrlPattern(String input, Pattern pattern, String mask, String prefix) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find() && matcher.groupCount() > 0)
        {
            input = input.replace(prefix + matcher.group(1), prefix + mask);
        }
        return input;

    }
}
