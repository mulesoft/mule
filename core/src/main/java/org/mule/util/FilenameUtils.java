/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.File;

public class FilenameUtils extends org.apache.commons.io.FilenameUtils
{
    public static File fileWithPathComponents(String[] pathComponents)
    {
        if (pathComponents == null)
        {
            return null;
        }
        
        StringBuffer buf = new StringBuffer(64);
        for (int i = 0; i < pathComponents.length; i++)
        {
            String component = pathComponents[i];
            if (component == null)
            {
                continue;
            }
            
            buf.append(component);
            if (i < pathComponents.length - 1)
            {
                buf.append(File.separator);
            }
        }
        return new File(buf.toString());
    }
    
    /**
     * Never create instances of this class.
     */
    private FilenameUtils()
    {
        super();
    }
}


