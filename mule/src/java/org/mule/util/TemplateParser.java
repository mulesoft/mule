/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>TemplateParser</code> is a simple string parser that will substitute
 * tokens in a string with values supplied in a Map.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TemplateParser
{
    public static final String ANT_TEMPLATE_STYLE = "ant";
    public static final String SQUARE_TEMPLATE_STYLE = "square";
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TemplateParser.class);

    private Pattern pattern = null;
    private int pre = 1;
    private int post = 1;
    private String style = null;


    public static TemplateParser createAntStyleParser() {
        return new TemplateParser(ANT_TEMPLATE_STYLE);
    }

    public static TemplateParser createSquareBracesStyleParser() {
        return new TemplateParser(SQUARE_TEMPLATE_STYLE);
    }

    private TemplateParser(String style) {
        if(ANT_TEMPLATE_STYLE.equals(style)) {
            pattern = Pattern.compile("\\$\\{[^\\}]+\\}");
            pre = 2;
        } else if(SQUARE_TEMPLATE_STYLE.equals(style)) {
            pattern = Pattern.compile("\\[[^\\]]+\\]");
        } else {
            throw new IllegalArgumentException("Unknown template style: " + style);
        }
        this.style = style;
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for a template
     * is not found in the map the template is left as is in the return String
     * @param props the key/value pairs to match against
     * @param template the string containing the template place holders i.e. My name is ${name}
     * @return the parsed String
     */
    public String parse(Map props, String template)
    {
        String result = template;
        Matcher m = pattern.matcher(template);
        String match, propname;
        Object value;
        while (m.find()) {
            match = m.group();
            propname = match.substring(pre, match.length() - post);
            value = props.get(propname);
            if (value == null) {
                if(logger.isWarnEnabled()) logger.warn("Value " + propname + " not found in context");
            } else {
                match = escape(match);
                result = result.replaceAll(match, value.toString());
            }
        }
        return result;
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for a template
     * is not found in the map the template is left as is in the return String
     * @param props the key/value pairs to match against
     * @param templates A List of templates
     * @return the parsed String
     */
    public List parse(Map props, List templates) {
        if(templates==null) {
            return new ArrayList();
        }
        List list = new ArrayList(templates.size());
        for (Iterator iterator = templates.iterator(); iterator.hasNext();) {
            list.add(parse(props, iterator.next().toString()));
        }
        return list;
    }

    /**
     * Matches one or more templates against a Map of key value pairs. If a value for a template
     * is not found in the map the template is left as is in the return String
     * @param props the key/value pairs to match against
     * @param templates A Map of templates.  The values for each map entry will be parsed
     * @return the parsed String
     */
    public Map parse(Map props, Map templates) {
        if(templates==null) {
            return new HashMap();
        }
        Map map = new HashMap(templates.size());
        Map.Entry entry;
        for (Iterator iterator = templates.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry)iterator.next();
            map.put(entry.getKey(), parse(props, entry.getValue().toString()));
        }
        return map;
    }

    private String escape(String string) {
        int length = string.length();
        if (length == 0) {
            // nothing to do
            return string;
        }
        else {
            StringBuffer buffer = new StringBuffer(length*2);
            for (int i = 0; i < length; i++) {
                char currentCharacter = string.charAt(i);
                switch (currentCharacter) {
                    case '[':
                    case ']':
                    case '{':
                    case '}':
                    case '$':
                        buffer.append("\\");
                        // fall through to append original character
                    default:
                        buffer.append(currentCharacter);
                }
            }
            return buffer.toString();
        }
    }

    public String getStyle() {
        return style;
    }

    public boolean isContainsTemplate(String value) {
        Matcher m = pattern.matcher(value);
        return m.find();
    }

}
