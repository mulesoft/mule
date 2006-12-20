/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.filters;

import org.mule.providers.file.FileConnector;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOMessage;

import java.io.File;
import java.io.FilenameFilter;

/**
 * <code>FilenameWildcardFilter</code> filters incoming files from a directory,
 * based on file patterns.
 */
public class FilenameWildcardFilter extends WildcardFilter implements FilenameFilter
{

    public FilenameWildcardFilter()
    {
        super();
    }

    public FilenameWildcardFilter(String pattern)
    {
        super(pattern);
    }

    /**
     * UMOFilter condition decider method. <p/> Returns
     * <code>boolean</code> <code>TRUE</code> if the file conforms to an
     * acceptable pattern or <code>FALSE</code> otherwise.
     * 
     * @param dir The directory to apply the filter to.
     * @param name The name of the file to apply the filter to.
     * @return indication of acceptance as boolean.
     */
    public boolean accept(File dir, String name)
    {
        if (name == null)
        {
            logger.warn("The filename and/or directory was null");
            return false;
        }
        else
        {
            return accept(name);
        }
    }

    public boolean accept(UMOMessage message)
    {
        return accept(message.getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
    }

}
