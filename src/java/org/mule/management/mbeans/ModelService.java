/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.MuleManager;
import org.mule.impl.MuleModel;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;

/**
 * <code>ModelService</code> exposes service information and actions on the
 * Mule Model
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ModelService implements ModelServiceMBean
{
    private MuleModel model;

    public ModelService()
    {
        model = (MuleModel)MuleManager.getInstance().getModel();

    }

    public void start() throws UMOException
    {
        model.start();
    }

    public void stop() throws UMOException
    {
        model.stop();
    }

    public void startComponent(String name) throws UMOException
    {
        model.startComponent(name);
    }

    public void stopComponent(String name) throws UMOException
    {
        model.stopComponent(name);
    }

    public void pauseComponent(String name) throws UMOException
    {
        model.pauseComponent(name);
    }

    public void resumeComponent(String name) throws UMOException
    {
        model.resumeComponent(name);
    }

    public void unregisterComponent(String name) throws UMOException
    {
        model.unregisterComponent(model.getDescriptor(name));
    }

    public boolean isComponentRegistered(String name) {
        return model.isComponentRegistered(name);
    }

    public UMODescriptor getComponentDescriptor(String name) {
       return model.getDescriptor(name);
    }

    public String getName() {
        return model.getName();
    }
}
