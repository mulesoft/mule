//COPYRIGHT
package org.mule.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>TemplateParser</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TemplateParser
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TemplateParser.class);

    private static Pattern tmplPattern = Pattern.compile("\\[[^\\]]+\\]");


    public static String parseString(Map props, String template) {
        String result = template;
        Matcher m = tmplPattern.matcher(template);
        String match, propname;
        Object value;
        while(m.find()) {
            match = m.group();
            propname = match.substring(1, match.length() -1);
            value = props.get(propname);
            if(value==null) {
                logger.error("Value " + propname + " not found in context");
                value = "";
            }
            result = result.replaceAll("\\[" + propname + "\\]", value.toString());
        }
        return result;
    }
}
