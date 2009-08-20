/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp.server;

import java.io.IOException;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;

/**
 * {@link Ftplet} implementation that calls methods on its callback. Although this seems a bit 
 * like jumping through hoops, it frees the individual test classes from having to deal with
 * creating custom Ftplets.
 */
public class MuleFtplet extends DefaultFtplet
{
    public interface Callback
    {
        void fileUploadCompleted();
        
        void fileMoveCompleted();
    }

    private Callback callback;
    
    public MuleFtplet(Callback callback)
    {
        super();
        this.callback = callback;
    }
    
    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException
    {
        callback.fileUploadCompleted();
        return null;
    }

    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException, IOException
    {
        callback.fileMoveCompleted();
        return null;
    }
}
