/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.util.Collection;
import java.util.Iterator;

import org.apache.ftpserver.ftplet.FileObject;

public class Directory extends Named
{

    public Directory(String name, ServerState state)
    {
        super(name, state);
    }

    public boolean isDirectory()
    {
        return true;
    }

    public boolean isFile()
    {
        return false;
    }

    public FileObject[] listFiles()
    {
        logger.debug("list files");
        Collection available = getState().getDownloadNames();
        FileObject[] files = new FileObject[available.size()];
        int index = 0;
        for (Iterator names = available.iterator(); names.hasNext();)
        {
            String name = (String) names.next();
            if (logger.isDebugEnabled())
            {
                logger.debug("file available: " + name);
            }
            files[index++] = new DownloadFile(name, getState());
        }
        return files;
    }

}
