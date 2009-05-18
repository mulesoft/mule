/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(TemplateParser.class);

    public static final Pattern ANT_TEMPLATE_PATTERN = Pattern.compile("\\$\\{[^\\}]+\\}");
    public static final Pattern SQUARE_TEMPLATE_PATTERN = Pattern.compile("\\[[^\\]]+\\]");
    public static final Pattern CURLY_TEMPLATE_PATTERN = Pattern.compile("\\{[^\\}]+\\}");
    public static final Pattern WIGGLY_MULE_TEMPLATE_PATTERN = Pattern.compile("#\\[[^#]+\\]");

    private final Pattern pattern;
    private final int pre;
    private final int post;
    private final String style;


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

    private TemplateParser(String style)
    {
        if (ANT_TEMPLATE_STYLE.equals(style))
        {
            pattern = ANT_TEMPLATE_PATTERN;
            pre = 2;
            post = 1;
        }
        else if (WIGGLY_MULE_TEMPLATE_STYLE.equals(style))
        {
            pattern = WIGGLY_MULE_TEMPLATE_PATTERN;
            pre = 2;
            post = 1;
        }
        else if (SQUARE_TEMPLATE_STYLE.equals(style))
        {
            pattern = SQUARE_TEMPLATE_PATTERN;
            pre = 1;
            post = 1;
        }
        else if (CURLY_TEMPLATE_STYLE.equals(style))
        {
            pattern = CURLY_TEMPLATE_PATTERN;
            pre = 1;
            post = 1;
        }
        else
        {
            throw new IllegalArgumentException("Unknown template style: " + style);
        }
        this.style = style;
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
    public String parse(Map props, String template)
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

    protected String parse(Map props, String template, TemplateCallback callback)
    {
        String result = template;
        Matcher m = pattern.matcher(template);

        while (m.find())
        {
            Object value = null;

            String match = m.group();
            String propname = match.substring(pre, match.length() - post);

            if (callback != null)
            {
                value = callback.match(propname);
            }
            else if (props != null)
            {
                value = props.get(propname);
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

                if (valueString.indexOf('\\') != -1)
                {
                    valueString = valueString.replaceAll("\\\\", "\\\\\\\\");
                }

                result = result.replaceAll(matchRegex, valueString);
            }
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
    public List parse(Map props, List templates)
    {
        if (templates == null)
        {
            return new ArrayList();
        }
        List list = new ArrayList(templates.size());
        for (Iterator iterator = templates.iterator(); iterator.hasNext();)
        {
            list.add(parse(props, iterator.next().toString()));
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
    public Map parse(final Map props, Map templates)
    {
        return parse(new TemplateCallback()
        {
            public Object match(String token)
            {
                return props.get(token);
            }
        }, templates);
    }

    public Map parse(TemplateCallback callback, Map templates)
    {
        if (templates == null)
        {
            return new HashMap();
        }
        Map map = new HashMap(templates.size());
        Map.Entry entry;
        for (Iterator iterator = templates.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry) iterator.next();
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
                        buffer.append("\\");
                        // fall through to append original character
                    default:
                        buffer.append(currentCharacter);
                }
            }
            return buffer.toString();
        }
    }

    public String getStyle()
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

    public static interface TemplateCallback
    {
        Object match(String token);
    }

}
