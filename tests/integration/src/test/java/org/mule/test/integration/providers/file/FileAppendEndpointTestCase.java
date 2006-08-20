/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * 
 * 
 * @author <a href="mailto:stephen.fenech@symphonysoft.com">Stephen Fenech</a>
 *
 */
public class FileAppendEndpointTestCase extends FunctionalTestCase{

    public void testBasic() throws Exception
    {
        MuleClient client=new MuleClient();
        (new File("myout/out.txt")).delete();
        client.send("vm://fileappend", "Hello1", null);
        client.send("vm://fileappend", "Hello2", null);
        assertEquals("Hello1Hello2\r\n",this.readFile("myout/out.txt"));
    }

    private String readFile(String path) throws IOException
    {
        FileReader fr = new FileReader(path);
        BufferedReader br=new BufferedReader(fr);

        String tmp;
        String email="";
        while((tmp=br.readLine())!=null)
        {
            email=email.concat(tmp)+"\r\n";
        }

        return email;
    }


    protected String getConfigResources() {
        return "mule-fileappend-connector-config.xml";
    }        
}
