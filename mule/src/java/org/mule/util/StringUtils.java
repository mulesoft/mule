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
 *
 */

package org.mule.util;

import java.util.StringTokenizer;

/**
 * <code>StringUtils</code> contains useful methods for manipulating Strings.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class StringUtils extends org.apache.commons.lang.StringUtils
{

    public static String[] split(String string, String delim)
    {
        StringTokenizer st = new StringTokenizer(string, delim);
        String[] results = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            results[i++] = st.nextToken().trim();
        }
        return results;
    }

}
