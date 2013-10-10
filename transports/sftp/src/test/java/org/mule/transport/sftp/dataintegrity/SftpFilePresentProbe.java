/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
