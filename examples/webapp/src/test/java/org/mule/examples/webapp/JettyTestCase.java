/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.webapp;

import org.mortbay.http.HttpContext;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;

/**
* This is a work in progress.  We may be better off using the maven-jetty-plugin in the "integration-tests"
* phase of the m2 lifecycle to test the WAR. 
*/
public class JettyTestCase extends AbstractWebappTestCase
{
    public static final String WEBAPP_WAR_FILE = "/home/travis/mule/examples/webapp/target/mule-examples.war";
    public static final String WEBAPP_CONTEXT_PATH = "/mule-examples";
    public static final int JETTY_PORT = 8090;
    
    Server jetty = null;
    
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        if (jetty == null)
        {
            // Jetty 5.x
            jetty = new Server();
            WebApplicationContext wc = new WebApplicationContext(WEBAPP_WAR_FILE);
            wc.setContextPath(WEBAPP_CONTEXT_PATH);
            wc.setWAR(WEBAPP_WAR_FILE);
            jetty.addContext(wc);
            
            // Jetty 6.x 
            //jetty = new Server(JETTY_PORT);
            //jetty.addHandler(new WebAppContext(WEBAPP_WAR_FILE, WEBAPP_CONTEXT_PATH));
    
            jetty.start();
        }
    }

    protected void suitePreTearDown() throws Exception
    {
        super.suitePreTearDown();
        if (jetty != null)
        {
            jetty.stop();
        }
    }
}
