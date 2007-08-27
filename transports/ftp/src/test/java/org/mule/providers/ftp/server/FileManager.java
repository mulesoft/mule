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

import org.apache.ftpserver.ftplet.FileSystemManager;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class FileManager implements FileSystemManager
{

    private ServerState state;

    public void setStateFromSystemProperties(String key)
    {
        state = (InOutState) System.getProperties().get(key);
        if (null == state)
        {
            throw new IllegalStateException("Missing state for key " + key);
        }
        state.started();
    }

    public FileSystemView createFileSystemView(User user) throws FtpException
    {
        return new FileView(state);
    }

}
