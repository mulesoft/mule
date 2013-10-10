/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.filters;

import org.mule.api.MuleMessage;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.file.FileConnector;

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
     * Filter condition decider method. <p/> Returns
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

    public boolean accept(MuleMessage message)
    {
        // TODO revisit, shouldn't it be looking in the inbound scope?
        Object filename = message.getOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME);
        return accept(filename);
    }

}
