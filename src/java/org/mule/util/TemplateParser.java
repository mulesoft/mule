/*
 * $Header$
 * $Revision$
 * $Date$
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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TemplateParser</code> is a simple string parser that will substitute
 * tokens in a string with values supplied in a Map.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TemplateParser
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TemplateParser.class);

    private Pattern pattern = null;
    private int pre = 1;
    private int post = 1;


    public static TemplateParser createAntStyleParser() {
        return new TemplateParser("ant");
    }

    public static TemplateParser createSquareBracesStyleParser() {
        return new TemplateParser("square");
    }

    private TemplateParser(String type) {
        if("ant".equals(type)) {
            pattern = Pattern.compile("\\$\\{[^\\}]+\\}");
            pre = 2;
        } else if("square".equals(type)) {
            pattern = Pattern.compile("\\[[^\\]]+\\]");
        }
    }
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
                logger.error("Value " + propname + " not found in context");
                value = "";
            }
            match = escape(match);
            result = result.replaceAll(match, value.toString());
        }
        return result;
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

}
