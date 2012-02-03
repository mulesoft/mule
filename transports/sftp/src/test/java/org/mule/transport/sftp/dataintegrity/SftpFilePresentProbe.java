
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
