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
    
    public static String maskPasswords(String xml)
    {
        xml = maskUrlPassword(xml, URL_PATTERN);
        xml = maskUrlPassword(xml, ADDRESS_PATTERN);

        Matcher matcher = PASSWORD_PATTERN.matcher(xml);
        if (matcher.find() && matcher.groupCount() > 0)
        {
            xml = xml.replaceAll(maskPasswordAttribute(matcher.group(1)), maskPasswordAttribute(PASSWORD_MASK));
        }
        xml = maskUrlPassword(xml, PASSWORD_PATTERN);

        return xml;
    }

    public static String maskUrlPassword(String xml, Pattern pattern)
    {
        return maskUrlPattern(xml, pattern, PASSWORD_MASK);
    }

    public static String maskUrlUserAndPassword(String input, Pattern passwordPattern, Pattern userPattern)
    {
        String inputMasked = maskUrlPattern(input, passwordPattern, PASSWORD_MASK, PASSWORD_URL_PREFIX);
        return maskUrlPattern(inputMasked, userPattern, USER_MASK, USER_URL_PREFIX);
    }

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
            input = input.replaceAll(prefix + matcher.group(1), prefix + mask);
        }
        return input;

    }
}
