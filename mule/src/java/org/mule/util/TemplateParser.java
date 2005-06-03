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

    private static Pattern tmplPattern = Pattern.compile("\\[[^\\]]+\\]");

    public static String parseString(Map props, String template)
    {
        String result = template;
        Matcher m = tmplPattern.matcher(template);
        String match, propname;
        Object value;
        while (m.find()) {
            match = m.group();
            propname = match.substring(1, match.length() - 1);
            value = props.get(propname);
            if (value == null) {
                logger.error("Value " + propname + " not found in context");
                value = "";
            }
            result = result.replaceAll("\\[" + propname + "\\]", value.toString());
        }
        return result;
    }
}
