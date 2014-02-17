/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.OutputHandler;

public class SftpOutputHandlerTetsCase extends AbstractSftpFunctionalTestCase {

    public static String TEST_PAYLOAD = "testing sftp OutputHandler support";
    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-write-file-config.xml";
    }
    
    @Test
    public void processOutputHandler() throws Exception
    {
        OutputHandler outputHandler = new OutputHandler() {
            
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException 
            {
                try 
                {
                    out.write(TEST_PAYLOAD.getBytes());
                } 
                catch (Exception e) 
                {
                   throw new IOException(e);
                }
                finally
                {
                    out.close();
                }
            }
        };
        MuleMessage muleMessage = muleContext.getClient().send("vm://overwrite", outputHandler, null);
        Assert.assertNull(muleMessage.getExceptionPayload());
        Assert.assertEquals(TEST_PAYLOAD,muleMessage.getPayloadAsString());
    }

}
