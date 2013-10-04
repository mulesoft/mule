/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;
import org.mule.api.model.Model;

/**
 * <code>ModelService</code> exposes service information and actions on the Mule
 * Model.
 */
@Deprecated
public class ModelService implements ModelServiceMBean
{
    private Model model;

    public ModelService(Model model)
    {
        this.model = model;

    }

    public void start() throws MuleException
    {
        model.start();
    }

    public void stop() throws MuleException
    {
        model.stop();
    }

//    public void startComponent(String name) throws MuleException
//    {
//        model.startComponent(name);
//    }
//
//    public void stopComponent(String name) throws MuleException
//    {
//        model.stopComponent(name);
//    }
//
//    public void pauseComponent(String name) throws MuleException
//    {
//        model.pauseComponent(name);
//    }
//
//    public void resumeComponent(String name) throws MuleException
//    {
//        model.resumeComponent(name);
//    }
//
//    public void unregisterComponent(String name) throws MuleException
//    {
//        muleContext.getRegistry().unregisterService(name);
//        //model.unregisterComponent(model.getDescriptor(name));
//    }
//
//    public boolean isComponentRegistered(String name)
//    {
//        return model.isComponentRegistered(name);
//    }
//
//    public UMODescriptor getComponentDescriptor(String name)
//    {
//        return muleContext.getRegistry().lookupService(name);
//
//        //return model.getDescriptor(name);
//    }

    public String getName()
    {
        return model.getName();
    }

    public String getType()
    {
        return model.getType();
    }
}
