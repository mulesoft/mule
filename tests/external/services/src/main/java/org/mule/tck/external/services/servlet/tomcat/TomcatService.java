/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.external.services.servlet.tomcat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.logger.SystemOutLogger;
import org.apache.catalina.startup.Embedded;
import org.apache.catalina.startup.ExpandWar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;

public class TomcatService
{
    private String baseDir;
    private String workDir;
    private String tempDir;
    private int port = 8080;
    private boolean readyToStart = false;
    private Embedded tomcat = null;
    private StandardHost host = null;
    private final Log logger = LogFactory.getLog(getClass());

    public TomcatService()
    {
    }

    public void deployWarFile(String contextName, URL warFile) throws Exception
    {
        if (tomcat != null)
        {
            if (!contextName.startsWith("/") && !contextName.equals(""))
            {
                contextName = "/" + contextName;
            }

            host.install(contextName, warFile);
            Context context = host.findDeployedApp(contextName);
            // optional Context configuration goes here
        }
        else
        {
            throw new Exception ("Unable to deploy warFile - Tomcat not initialised!");
        }
    }

    public String getBaseDir()
    {
        return baseDir;
    }

    public void setBaseDir(String baseDir)
    {
        this.baseDir = baseDir;
        System.setProperty("catalina.home", baseDir);

        // TODO - change these
        tempDir = System.getProperty("java.io.tmpdir");
        workDir = baseDir + "/work";
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void init()
    {
        logger.info("Tomcat init start");

        try
        {
            tomcat = new Embedded();

            // This seems to throw a NPE here
            //tomcat.setDebug(9);

            Engine engine = tomcat.createEngine();
            engine.setName("MuleTestTomcatEngine");
            engine.setDefaultHost("localhost");

            host =
                (StandardHost)tomcat.createHost("localhost", baseDir + "/apps");
            host.setWorkDir(workDir);
            engine.addChild(host);

            tomcat.addEngine(engine);
            Connector connector =
                tomcat.createConnector((InetAddress)null, port, false);

            tomcat.addConnector(connector);
            logger.info("Tomcat is ready to start");
            readyToStart = true;
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    public void start() throws Exception
    {
        if (readyToStart)
        {
            tomcat.start();
        }
        else
        {
            throw new Exception("Tomcat failed to initialise - not starting");
        }
            BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Type something:");
                String input = is.readLine();
                break;
            }
    }

    public void stop() throws Exception
    {
        tomcat.stop();
    }

    /*
    public static void main(String[] args) {
        try {
            EmbeddedTomcat tomcat = new EmbeddedTomcat();
            tomcat.setBaseDir("c:/test-tomcat");
            tomcat.deployWarFile("file:C:/hello.war");
            tomcat.start();
            BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Type something:");
                String input = is.readLine();
                break;
            }
            tomcat.stop();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    */
}

