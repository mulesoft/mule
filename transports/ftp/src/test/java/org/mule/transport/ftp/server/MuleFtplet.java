/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
