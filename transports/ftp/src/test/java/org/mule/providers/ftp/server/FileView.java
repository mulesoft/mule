/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ftpserver.ftplet.FileObject;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;

public class FileView implements FileSystemView
{

    protected final Log logger = LogFactory.getLog(getClass());
    private CountDownLatch started = new CountDownLatch(1);
    private ServerState state;

    public FileView(ServerState state)
    {
        this.state = state;
    }

    public void flagStarted(int count)
    {
        started.countDown();
    }

    public FileObject getHomeDirectory() throws FtpException
    {
        return new Directory("/", state);
    }

    public FileObject getCurrentDirectory() throws FtpException
    {
        return new Directory("/", state);
    }

    public boolean changeDirectory(String dir) throws FtpException
    {
        return true;
    }

    public FileObject getFileObject(String name) throws FtpException
    {
        logger.debug("request for: " + name);
        if (state.getDownloadNames().contains(name))
        {
            return new DownloadFile(name, state);
        }
        // TODO - is this standard FTP convention?
        else if (null != name && name.endsWith("/"))
        {
            return new Directory(name, state);
        }
        else
        {
            return new UploadFile(name, state);
        }
    }

    public boolean isRandomAccessible() throws FtpException
    {
        return true;
    }

    public void dispose()
    {
        // no-op
    }

}
