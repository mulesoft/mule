/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.xml;

import org.mule.runtime.extension.api.introspection.ExtensionModel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Utilities for manipulating names of {@link ExtensionModel extensions} and
 * their components
 *
 * @since 3.7.0
 */
public class NameUtils
{

    private static final List<Inflection> plural = new ArrayList<>();
    private static final List<String> uncountable = new ArrayList<>();

    static
    {
        // plural is "singular to plural form"
        // singular is "plural to singular form"
        plural("$", "s");
        plural("s$", "s");
        plural("(ax|test)is$", "$1es");
        plural("(octop|vir)us$", "$1i");
        plural("(alias|status)$", "$1es");
        plural("(bu)s$", "$1ses");
        plural("(buffal|tomat)o$", "$1oes");
        plural("([ti])um$", "$1a");
        plural("sis$", "ses");
        plural("(?:([^f])fe|([lr])f)$", "$1$2ves");
        plural("(hive)$", "$1s");
        plural("([^aeiouy]|qu)y$", "$1ies");
        plural("(x|ch|ss|sh)$", "$1es");
        plural("(matr|vert|ind)ix|ex$", "$1ices");
        plural("([m|l])ouse$", "$1ice");
        plural("^(ox)$", "$1en");
        plural("(quiz)$", "$1zes");

        uncountable("equipment");
        uncountable("information");
        uncountable("rice");
        uncountable("money");
        uncountable("species");
        uncountable("series");
        uncountable("fish");
        uncountable("sheep");
    }

    private NameUtils()
    {
    }

    /**
     * Registers a plural {@code replacement} for the given {@code pattern}
     *
     * @param pattern     the pattern for which you want to register a plural form
     * @param replacement the replacement pattern
     */
    private static void plural(String pattern, String replacement)
    {
        plural.add(0, new Inflection(pattern, replacement));
    }

    private static void uncountable(String word)
    {
        uncountable.add(word);
    }

    /**
     * Transforms a camel case value into a hyphenizedone.
     * <p>
     * For example:
     * {@code messageProcessor} would be transformed to {@code message-processor}
     *
     * @param camelCaseName a {@link String} in camel case form
     * @return the {@code camelCaseName} in hypenized form
     */
    public static String hyphenize(String camelCaseName)
    {
        if (StringUtils.isBlank(camelCaseName))
        {
            return camelCaseName;
        }

        String result = "";
        String[] parts = camelCaseName.split("(?<!^)(?=[A-Z])");

        for (int i = 0; i < parts.length; i++)
        {
            result += parts[i].toLowerCase() + (i < parts.length - 1 ? "-" : "");
        }

        return result;
    }

    /**
     * Return the pluralized version of a word.
     *
     * @param word The word
     * @return The pluralized word
     */
    public static String pluralize(String word)
    {
        if (isUncountable(word))
        {
            return word;
        }
        else
        {
            for (Inflection inflection : plural)
            {
                if (inflection.match(word))
                {
                    return inflection.replace(word);
                }
            }
            return word;
        }
    }

    /**
     * Return true if the word is uncountable.
     *
     * @param word The word
     * @return True if it is uncountable
     */
    public static boolean isUncountable(String word)
    {
        if (StringUtils.isBlank(word))
        {
            for (String w : uncountable)
            {
                if (w.equalsIgnoreCase(word))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static class Inflection
    {

        private String pattern;
        private String replacement;
        private boolean ignoreCase;

        public Inflection(String pattern, String replacement)
        {
            this(pattern, replacement, true);
        }

        public Inflection(String pattern, String replacement, boolean ignoreCase)
        {
            this.pattern = pattern;
            this.replacement = replacement;
            this.ignoreCase = ignoreCase;
        }


        /**
         * Does the given word match?
         *
         * @param word The word
         * @return True if it matches the inflection pattern
         */
        public boolean match(String word)
        {
            int flags = 0;
            if (ignoreCase)
            {
                flags = flags | java.util.regex.Pattern.CASE_INSENSITIVE;
            }
            return java.util.regex.Pattern.compile(pattern, flags).matcher(word).find();
        }

        /**
         * Replace the word with its pattern.
         *
         * @param word The word
         * @return The result
         */
        public String replace(String word)
        {
            int flags = 0;
            if (ignoreCase)
            {
                flags = flags | java.util.regex.Pattern.CASE_INSENSITIVE;
            }
            return java.util.regex.Pattern.compile(pattern, flags).matcher(word).replaceAll(replacement);
        }
    }
}
