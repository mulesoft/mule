/*
 * $Id:VisualizerService.java 7555 2007-07-18 03:17:16Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.visualizer.service;

import org.mule.RegistryContext;
import org.mule.config.ConfigurationException;
import org.mule.impl.MuleMessage;
import org.mule.impl.UMODescriptorAware;
import org.mule.tools.visualizer.MuleVisualizer;
import org.mule.tools.visualizer.config.GraphConfig;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.maven.MuleVisualizerPlugin;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

/** TODO */
public class VisualizerService extends MuleVisualizerPlugin implements Callable, Initialisable, UMODescriptorAware
{
    protected GraphConfig config;
    protected GraphEnvironment environment;
    protected MuleVisualizer visualizer;
    protected UMODescriptor descriptor;

    public void setDescriptor(UMODescriptor descriptor) throws ConfigurationException
    {
        this.descriptor = descriptor;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            config = buildConfig();
            config.setOutputDirectory(RegistryContext.getRegistry().getConfiguration().getWorkingDirectory() + File.pathSeparator + "visualizer");
            environment = new GraphEnvironment(config);
            visualizer = new MuleVisualizer(environment);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        UMOMessage msg = eventContext.getMessage();
        Set names = msg.getAttachmentNames();
        if(names.size()==0)
        {
            throw new IllegalArgumentException("There were no files attached to process");
        }

        List files = new ArrayList(names.size());
        for (Iterator iterator = names.iterator(); iterator.hasNext();)
        {
            String s = (String) iterator.next();
            DataHandler dh = msg.getAttachment(s);
            if(dh.getDataSource().getContentType().startsWith("text/xml"))
            {
                files.add(dh.getInputStream());
            }
        }
        if(files.size()==0)
        {
            throw new IllegalArgumentException("There were no Xml attachments for email: " + msg.getProperty("subject"));
        }
        List results = visualizer.visualize(files);
        UMOMessage result = new MuleMessage("Thanks for using Mule Visualizer!");
        if(results==null)
        {
            return null;
        }
        
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            String s = (String) iterator.next();
            File f= new File(s);
            FileDataSource ds = new FileDataSource(f);
            result.addAttachment(f.getName(), new DataHandler(ds));
        }

        //Also attache the source files
        for (Iterator iterator = names.iterator(); iterator.hasNext();)
        {
            String s = (String) iterator.next();
            result.addAttachment(s, msg.getAttachment(s));
        }
        
        return result;
    }


}
