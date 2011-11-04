/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TemplateParser</code> is a simple string parser that will substitute
 * tokens in a string with values supplied in a Map.
 */
public final class TemplateParser
{
    public static final String ANT_TEMPLATE_STYLE = "ant";
    public static final String SQUARE_TEMPLATE_STYLE = "square";
    public static final String CURLY_TEMPLATE_STYLE = "curly";
    public static final String WIGGLY_MULE_TEMPLATE_STYLE = "mule";

    private static final String DOLLAR_ESCAPE = "@@@";

    private static final Map<String, PatternInfo> patterns = new HashMap<String, PatternInfo>();

    static
    {
        patterns.put(ANT_TEMPLATE_STYLE, new PatternInfo(ANT_TEMPLATE_STYLE, "\\$\\{[^\\}]+\\}", "${", "}"));
        patterns.put(SQUARE_TEMPLATE_STYLE, new PatternInfo(SQUARE_TEMPLATE_STYLE, "\\[[^\\]]+\\]", "[", "]"));
        patterns.put(CURLY_TEMPLATE_STYLE, new PatternInfo(CURLY_TEMPLATE_STYLE, "\\{[^\\}]+\\}", "{", "}"));

        // Such a complex regex is needed to support nested expressions, otherwise we
        // have to do this manually or using an ANTLR grammar etc.

        // Support for 3 levels (2 nested)
        patterns.put(WIGGLY_MULE_TEMPLATE_STYLE, new PatternInfo(WIGGLY_MULE_TEMPLATE_STYLE,
            "#\\[((?:#\\[(?:#\\[.*?\\]|\\[.*?\\]|.)*?\\]|\\[.*?\\]|.)*?)\\]", "#[", "]"));

        // Support for 2 levels (1 nested)
        // "#\\[((?:#\\[.*?\\]|\\[.*?\\]|.)*?)\\]"
    }

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(TemplateParser.class);

    public static final Pattern ANT_TEMPLATE_PATTERN = patterns.get(ANT_TEMPLATE_STYLE).getPattern();
    public static final Pattern SQUARE_TEMPLATE_PATTERN = patterns.get(SQUARE_TEMPLATE_STYLE).getPattern();
    public static final Pattern CURLY_TEMPLATE_PATTERN = patterns.get(CURLY_TEMPLATE_STYLE).getPattern();
    public static final Pattern WIGGLY_MULE_TEMPLATE_PATTERN = patterns.get(WIGGLY_MULE_TEMPLATE_STYLE).getPattern();

    private final Pattern pattern;
    private final int pre;
    private final int post;
    private final PatternInfo style;


    public static TemplateParser createAntStyleParser()
    {
        return new TemplateParser(ANT_TEMPLATE_STYLE);
    }

    public static TemplateParser createSquareBracesStyleParser()
    {
        return new TemplateParser(SQUARE_TEMPLATE_STYLE);
    }

    public static TemplateParser createCurlyBracesStyleParser()
    {
        return new TemplateParser(CURLY_TEMPLATE_STYLE);
    }

    public static TemplateParser createMuleStyleParser()
    {
        return new TemplateParser(WIGGLY_MULE_TEMPLATE_STYLE);
    }

    private TemplateParser(String styleName)
    {
        this.style = patterns.get(styleName);
        if (this.style == null)
        {
            throw new IllegalArgumentException("Unknown template style: " + styleName);

        }
        pattern = style.getPattern();
        pre = style.getPrefix().length();
        post = style.getSuffix().length();
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for
     * a template is not found in the map the template is left as is in the return
     * String
     *
     * @param props    the key/value pairs to match against
     * @param template the string containing the template place holders i.e. My name
     *                 is ${name}
     * @return the parsed String
     */
    public String parse(Map<?, ?> props, String template)
    {
        return parse(props, template, null);
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for
     * a template is not found in the map the template is left as is in the return
     * String
     *
     * @param callback a callback used to resolve the property name
     * @param template the string containing the template place holders i.e. My name
     *                 is ${name}
     * @return the parsed String
     */
    public String parse(TemplateCallback callback, String template)
    {
        return parse(null, template, callback);
    }

    protected String parse(Map<?, ?> props, String template, TemplateCallback callback)
    {
        String result = template;
        Map<?, ?> newProps = props;
        if (props != null && !(props instanceof CaseInsensitiveHashMap))
        {
            newProps = new CaseInsensitiveHashMap(props);
        }

        Matcher m = pattern.matcher(result);

        while (m.find())
        {
            Object value = null;

            String match = m.group();
            String propname = match.substring(pre, match.length() - post);

            if (callback != null)
            {
                value = callback.match(propname);
            }
            else if (newProps != null)
            {
                value = newProps.get(propname);
            }

            if (value == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Value " + propname + " not found in context");
                }
            }
            else
            {
                String matchRegex = escape(match);
                String valueString = value.toString();
                //need to escape $ as they resolve into group references, escaping them was not enough
                //This smells a bit like a hack, but one way or another these characters need to be escaped
                if (valueString.indexOf('$') != -1)
                {
                    valueString = valueString.replaceAll("\\$", DOLLAR_ESCAPE);
                }

                if (valueString.indexOf('\\') != -1)
                {
                    valueString = valueString.replaceAll("\\\\", "\\\\\\\\");
                }

                result = result.replaceAll(matchRegex, valueString);
            }
        }
        if (result.indexOf(DOLLAR_ESCAPE) != -1)
        {
            result = result.replaceAll(DOLLAR_ESCAPE, "\\$");
        }
        return result;
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for
     * a template is not found in the map the template is left as is in the return
     * String
     *
     * @param props     the key/value pairs to match against
     * @param templates A List of templates
     * @return the parsed String
     */
    public List<?> parse(Map<?, ?> props, List<?> templates)
    {
        if (templates == null)
        {
            return new ArrayList<Object>();
        }

        List<String> list = new ArrayList<String>(templates.size());
        for (Object tmpl : templates)
        {
            list.add(parse(props, tmpl.toString()));
        }
        return list;
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for
     * a template is not found in the map the template is left as is in the return
     * String
     *
     * @param props     the key/value pairs to match against
     * @param templates A Map of templates. The values for each map entry will be
     *                  parsed
     * @return the parsed String
     */
    public Map<?, ?> parse(final Map<?, ?> props, Map<?, ?> templates)
    {
        return parse(new TemplateCallback()
        {
            public Object match(String token)
            {
                return props.get(token);
            }
        }, templates);
    }

    public Map<?, ?> parse(TemplateCallback callback, Map<?, ?> templates)
    {
        if (templates == null)
        {
            return new HashMap<Object, Object>();
        }

        Map<Object, String> map = new HashMap<Object, String>(templates.size());
        for (Map.Entry<?, ?> entry : templates.entrySet())
        {
            map.put(entry.getKey(), parse(callback, entry.getValue().toString()));
        }
        return map;
    }

    private String escape(String string)
    {
        int length = string.length();
        if (length == 0)
        {
            // nothing to do
            return string;
        }
        else
        {
            StringBuffer buffer = new StringBuffer(length * 2);
            for (int i = 0; i < length; i++)
            {
                char currentCharacter = string.charAt(i);
                switch (currentCharacter)
                {
                    case '[':
                    case ']':
                    case '{':
                    case '}':
                    case '(':
                    case ')':
                    case '$':
                    case '#':
                    case '*':
                    case '?':
                        buffer.append("\\");
                        //$FALL-THROUGH$ to append original character
                    default:
                        buffer.append(currentCharacter);
                }
            }
            return buffer.toString();
        }
    }

    public PatternInfo getStyle()
    {
        return style;
    }

    public boolean isContainsTemplate(String value)
    {
        if (value == null)
        {
            return false;
        }

        Matcher m = pattern.matcher(value);
        return m.find();
    }

    public boolean isValid(String expression)
    {
        try
        {
            style.validate(expression);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    public void validate(String expression) throws IllegalArgumentException
    {
        style.validate(expression);
    }

    public static interface TemplateCallback
    {
        Object match(String token);
    }


    public static class PatternInfo
    {
        String name;
        String regEx;
        String prefix;
        String suffix;

        PatternInfo(String name, String regEx, String prefix, String suffix)
        {
            this.name = name;
            this.regEx = regEx;
            if (prefix.length() < 1 || prefix.length() > 2)
            {
                throw new IllegalArgumentException("Prefix can only be one or two characters long: " + prefix);
            }
            this.prefix = prefix;
            if (suffix.length() != 1)
            {
                throw new IllegalArgumentException("Suffic can only be one character long: " + suffix);
            }
            this.suffix = suffix;
        }

        public String getRegEx()
        {
            return regEx;
        }

        public String getPrefix()
        {
            return prefix;
        }

        public String getSuffix()
        {
            return suffix;
        }

        public String getName()
        {
            return name;
        }

        public Pattern getPattern()
        {
            return Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
        }

        public void validate(String expression) throws IllegalArgumentException
        {
            Stack<Character> openDelimiterStack = new Stack<Character>();

            int charCount = expression.length();
            int index = 0;
            char nextChar = ' ';
            char preDelim = 0;
            char open;
            char close;
            boolean inExpression = false;
            int expressionCount = 0;
            if (prefix.length() == 2)
            {
                preDelim = prefix.charAt(0);
                open = prefix.charAt(1);
            }
            else
            {
                open = prefix.charAt(0);
            }
            close = suffix.charAt(0);

            for (; index < charCount; index++)
            {
                nextChar = expression.charAt(index);
                if (preDelim != 0 && nextChar == preDelim)
                {
                    //escaped
                    if (inExpression)
                    {
                        if (index < charCount && expression.charAt(index + 1) == open)
                        {
                            throw new IllegalArgumentException(String.format("Character %s at position %s suggests an expression inside an expression", open, index));
                        }
                    }
                    else if (openDelimiterStack.isEmpty())
                    {
                        openDelimiterStack.push(nextChar);
                        nextChar = expression.charAt(++index);
                        if (nextChar != open)
                        {
                            throw new IllegalArgumentException(String.format("Character %s at position %s must appear immediately after %s", open, index, preDelim));
                        }
                        inExpression = true;

                    }
                    else
                    {
                        throw new IllegalArgumentException(String.format("Character %s at position %s appears out of sequence. Character cannot appear after %s", nextChar, index, openDelimiterStack.pop()));
                    }
                }

                if (nextChar == open)
                {
                    if (preDelim == 0 || inExpression)
                    {
                        openDelimiterStack.push(nextChar);
                    }
                    //Check the stack size to avoid out of bounds
                    else if (openDelimiterStack.size() == 1 && openDelimiterStack.peek().equals(preDelim))
                    {
                        openDelimiterStack.push(nextChar);
                    }
                    else
                    {
                        throw new IllegalArgumentException(String.format("Character %s at position %s appears out of sequence. Character cannot appear after %s", nextChar, index, preDelim));
                    }
                }
                else if (nextChar == close)
                {
                    if (openDelimiterStack.isEmpty())
                    {
                        throw new IllegalArgumentException(String.format("Character %s at position %s appears out of sequence", nextChar, index));
                    }
                    else
                    {
                        openDelimiterStack.pop();
                        if (preDelim != 0 && openDelimiterStack.peek() == preDelim)
                        {
                            openDelimiterStack.pop();
                        }


                        if (openDelimiterStack.isEmpty())
                        {
                            inExpression = false;
                            expressionCount++;
                            //throw new IllegalArgumentException(String.format("Character %s at position %s appears out of sequence", nextChar, index));
                        }
                    }
                }
            }
            if (expressionCount == 0)
            {
                throw new IllegalArgumentException("Not an expression: " + expression);
            }
        }

    }
}
