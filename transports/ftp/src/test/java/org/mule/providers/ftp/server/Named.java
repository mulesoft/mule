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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ftpserver.ftplet.FileObject;

public abstract class Named implements FileObject 
{

    protected final Log logger = LogFactory.getLog(getClass());
    private String name;
    private ServerState state;

    public Named(String name, ServerState state)
    {
        this.name = name;
        this.state = state;
        if (logger.isDebugEnabled())
        {
            logger.debug("created: " + name);
        }
    }

    public String getFullName()
    {
        return name;
    }

    public String getShortName()
    {
        return name;
    }

    public boolean isHidden()
    {
        return false;
    }

    public boolean isDirectory()
    {
        return true;
    }

    public boolean doesExist()
    {
        return true;
    }

    public boolean hasReadPermission()
    {
        return true;
    }

    public boolean hasWritePermission()
    {
        return true;
    }

    public boolean hasDeletePermission()
    {
        return false;
    }

    public String getOwnerName()
    {
        return null;
    }

    public String getGroupName()
    {
        return null;
    }

    public int getLinkCount()
    {
        return 0;
    }

    public long getLastModified()
    {
        return 0;
    }

    public long getSize()
    {
        return 0;
    }

    public boolean mkdir()
    {
        return false;
    }

    public boolean delete()
    {
        return false;
    }

    public boolean move(FileObject destination)
    {
        return false;
    }

    protected ServerState getState()
    {
        return state;
    }

    public OutputStream createOutputStream(long offset) throws IOException
    {
        return null;
    }

    public InputStream createInputStream(long offset) throws IOException
    {
        return null;
    }

}
