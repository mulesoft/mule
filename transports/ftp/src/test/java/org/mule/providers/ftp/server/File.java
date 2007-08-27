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

import org.apache.ftpserver.ftplet.FileObject;

public class File extends Named
{

    public File(String name, ServerState state)
    {
        super(name, state);
    }

    public boolean isDirectory()
    {
        return false;
    }

    public boolean isFile()
    {
        return true;
    }

    public FileObject[] listFiles()
    {
        return new FileObject[0];
    }

}
