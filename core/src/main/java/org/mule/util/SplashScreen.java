/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.MuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements singleton pattern to allow different splash-screen implementations
 * following the concept of header, body, and footer. Header and footer are
 * reserved internally to Mule but body can be used to customize splash-screen
 * output. External code can e.g. hook into the start-up splash-screen as follows:
 * <pre><code>
 *   SplashScreen splashScreen = SplashScreen.getInstance(ServerStartupSplashScreen.class);
 *   splashScreen.addBody("Some extra text");
 * </code></pre>
 */
public abstract class SplashScreen
{
    protected List<String> header = new ArrayList<String>();
    protected List<String> body = new ArrayList<String>();
    protected List<String> footer = new ArrayList<String>();
    
    /**
     * Setting the header clears body and footer assuming a new
     * splash-screen is built.
     * 
     */
    final public void setHeader(MuleContext context)
    {
        header.clear();
        doHeader(context);
    }
    
    final public void addBody(String line)
    {
        doBody(line);
    }
    
    final public void setFooter(MuleContext context)
    {
        footer.clear();
        doFooter(context);
    }

    public static String miniSplash(final String message)
    {
        // middle dot char
        return StringMessageUtils.getBoilerPlate(message, '+', 60);
    }

    protected void doHeader(MuleContext context)
    {
        // default reserved for mule core info
    }   
    
    protected void doBody(String line)
    {
        body.add(line);
    }

    protected void doFooter(MuleContext context)
    {
        // default reserved for mule core info
    }    
    
    public String toString()
    {
        List<String> boilerPlate = new ArrayList<String>(header);
        boilerPlate.addAll(body);
        boilerPlate.addAll(footer);
        return StringMessageUtils.getBoilerPlate(boilerPlate, '*', 70);
    }
    
    protected SplashScreen()
    {
        // make sure no one else creates an instance
    }
}
