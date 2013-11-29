package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

public class SftpWriteFileTestCase extends AbstractSftpFunctionalTestCase {
    
    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-write-file-config.xml";
    }    
    
    @Test
    public void appendFile() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://append", "hello", null);
        client.send("vm://append", " world", null);
        MuleMessage message = client.request("file://testdir/append.txt", 3000);
        assertEquals("hello world", message.getPayloadAsString());
    }

    @Test
    public void overwriteFile() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://overwrite", "hello", null);
        client.send("vm://overwrite", "world", null);
        MuleMessage message = client.request("file://testdir/overwrite.txt", 3000);
        assertEquals("world", message.getPayloadAsString());
    }

}
