/*
 * $Id: CxfBasicTestCase.java 11449 2008-03-20 12:27:50Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class DispatchTestCase extends FunctionalTestCase
{
    public void testEchoService() throws Exception
    {
        final MuleClient client = new MuleClient();
        
        final byte[] buf = new byte[8192];
        for (int b = 0; b < buf.length; b++) 
        {
            buf[b] = 'a';
        }
        
        client.send("http://localhost:63081/services/Echo", 
            new DefaultMuleMessage(new ByteArrayInputStream(buf)));

        for (int t = 0; t < 10; t++)
        {
            new Thread(new Runnable() 
            {
                public void run()
                {
                    Map<String,Object> props = new HashMap<String, Object>();
                    props.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "vm://queue");
                    try
                    {
                        for (int i = 0; i < 20; i++) 
                        {
                                client.dispatch("http://localhost:63081/services/Echo", 
                                    new DefaultMuleMessage(buf),
                                    props);
                            
                        }
                    }
                    catch (MuleException e)
                    {
                        e.printStackTrace();
                    }
                }
                
            }).start();
        }
        
        int count = 0;
        while (count < 200 && client.request("vm://queue", 5000) != null) 
        {
            count++;
        }
        
        assertEquals(20*10, count);
    }
    
    protected String getConfigResources()
    {
        return "dispatch-conf.xml";
    }

}
