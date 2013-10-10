/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        return FileUtils.newFile(buf.toString());
    }
    
    /**
     * Never create instances of this class.
     */
    private FilenameUtils()
    {
        super();
    }
}


