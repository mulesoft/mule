/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import org.mule.tck.probe.Probe;
import org.mule.transport.sftp.SftpClient;

import java.io.IOException;
import java.util.Arrays;

public class SftpFilePresentProbe implements Probe
{

    private SftpClient client;
    private String dir;
    private String filename;

    public SftpFilePresentProbe(SftpClient sftpClient, String dir, String filename)
    {
        this.client = sftpClient;
        this.dir = dir;
        this.filename = filename;
    }

    @Override
    public boolean isSatisfied()
    {
        try
        {
            return Arrays.asList(client.listFiles(dir)).contains(filename);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not read SFTP directory [" + dir + "]", e);
        }
    }

    @Override
    public String describeFailure()
    {
        return "File [" + filename + "] not found on SFTP directory [" + dir + "]";
    }

}
