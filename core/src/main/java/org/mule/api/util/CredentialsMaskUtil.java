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

/**
 * Utils to mask credentials
 */
public final class CredentialsMaskUtil
{

    public static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
    public static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
    public static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"]*)\"");
    public static final Pattern PASSWORD_PATTERN_NO_QUOTES = compile("password=([^\\s;]+)");
    public static final Pattern USER_PATTERN_NO_QUOTES = compile("user=([^\\s;]+)");
    public static final String PASSWORD_MASK = "<<credentials>>";
    public static final String USER_MASK = "<<user>>";
    public static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

    private static final String USER_URL_PREFIX = "user=";
    private static final String PASSWORD_URL_PREFIX = "password=";

    private CredentialsMaskUtil()
    {

    }

    /**
     * Masks URL credentials
     * 
     * @param input input for credentials to be masked
     * @return input with password masked
     */
    public static String maskPasswords(String input)
    {
        input = maskUrlPassword(input, URL_PATTERN);
        input = maskUrlPassword(input, ADDRESS_PATTERN);

        Matcher matcher = PASSWORD_PATTERN.matcher(input);
        if (matcher.find() && matcher.groupCount() > 0)
        {
            input = input.replaceAll(maskPasswordAttribute(matcher.group(1)), maskPasswordAttribute(PASSWORD_MASK));
        }
        input = maskUrlPassword(input, PASSWORD_PATTERN);

        return input;
    }

    /**
     * Masks password in input
     * 
     * @param input credentials for password to be masked
     * @param pattern password pattern
     * @return input with password masked
     */
    public static String maskUrlPassword(String input, Pattern pattern)
    {
        return maskUrlPattern(input, pattern, PASSWORD_MASK);
    }

    /**
     * Masks user and password in text
     * 
     * @param textToMask text for user and password to be masked
     * @param passwordPattern password pattern
     * @param userPattern user pattern
     * @return text masked
     */
    public static String maskUserAndPassword(String textToMask, Pattern passwordPattern, Pattern userPattern)
    {
        String textMasked = maskUrlPattern(textToMask, passwordPattern, PASSWORD_MASK, PASSWORD_URL_PREFIX);
        return maskUrlPattern(textMasked, userPattern, USER_MASK, USER_URL_PREFIX);
    }

    /**
     * Masks password attribute
     * 
     * @param password password to be masked
     * @return password masked
     */
    public static String maskPasswordAttribute(String password)
    {
        return format(PASSWORD_ATTRIBUTE_MASK, password);
    }

    /**
     * Masks pattern in text
     * 
     * @param textToMask text to be masked
     * @param pattern pattern to find the tokens to be masked
     * @param mask mask to use
     * @return masked text
     */
    public static String maskUrlPattern(String textToMask, Pattern pattern, String mask)
    {
        return maskUrlPattern(textToMask, pattern, mask, "");
    }

    private static String maskUrlPattern(String textToMask, Pattern pattern, String mask, String prefix)
    {
        Matcher matcher = pattern.matcher(textToMask);
        if (matcher.find() && matcher.groupCount() > 0)
        {
            textToMask = textToMask.replaceAll(prefix + matcher.group(1), prefix + mask);
        }
        return textToMask;

    }
}
