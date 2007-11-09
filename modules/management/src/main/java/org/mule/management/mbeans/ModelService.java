/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;

/**
 * <code>ModelService</code> exposes service information and actions on the Mule
 * Model.
 */
public class ModelService implements ModelServiceMBean
{
    private UMOModel model;

    public ModelService(UMOModel model)
    {
        this.model = model;

    }

    public void start() throws UMOException
    {
        model.start();
    }

    public void stop() throws UMOException
    {
        model.stop();
    }

//    public void startComponent(String name) throws UMOException
//    {
//        model.startComponent(name);
//    }
//
//    public void stopComponent(String name) throws UMOException
//    {
//        model.stopComponent(name);
//    }
//
//    public void pauseComponent(String name) throws UMOException
//    {
//        model.pauseComponent(name);
//    }
//
//    public void resumeComponent(String name) throws UMOException
//    {
//        model.resumeComponent(name);
//    }
//
//    public void unregisterComponent(String name) throws UMOException
//    {
//        RegistryContext.getRegistry().unregisterService(name);
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
//        return RegistryContext.getRegistry().lookupService(name);
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
