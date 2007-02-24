/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model;

import org.mule.RegistryContext;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;

import java.util.Iterator;
import java.util.Map;

/**
 * @deprecated This functionality should be moved to the registry
 */
public class ModelHelper
{
    public static final String SYSTEM_MODEL = "_system";

    public static String getSystemModelType()
    {
        return "seda";
    }
    
    public static boolean isComponentRegistered(String name)
    {
        for (Iterator iterator = RegistryContext.getRegistry().getModels().values().iterator(); iterator.hasNext();)
        {
            UMOModel m =  (UMOModel)iterator.next();
            if(m.isComponentRegistered(name))
            {
                return true;
            }
        }
        return false;
    }

    public static UMOComponent getComponent(String name)
    {
        for (Iterator iterator = RegistryContext.getRegistry().getModels().values().iterator(); iterator.hasNext();)
        {
            UMOModel m =  (UMOModel)iterator.next();
            if(m.isComponentRegistered(name))
            {
                return m.getComponent(name);
            }
        }
        return null;
    }

    public static UMODescriptor getDescriptor(String name)
    {
        for (Iterator iterator = RegistryContext.getRegistry().getModels().values().iterator(); iterator.hasNext();)
        {
            UMOModel m =  (UMOModel)iterator.next();
            if(m.isComponentRegistered(name))
            {
                return m.getDescriptor(name);
            }
        }
        return null;
    }

    //TODO RM*: Move this method
    public static void registerSystemComponent(UMODescriptor d) throws UMOException
    {
        UMOModel model = RegistryContext.getRegistry().lookupModel(SYSTEM_MODEL);
        if(model==null)
        {
            model = ModelFactory.createModel("seda");
            model.setName(SYSTEM_MODEL);
            RegistryContext.getRegistry().registerModel(model);
        }
        model.registerComponent(d);
    }

    public static UMOModel getFirstUserModel() throws UMOException
    {
        Map models = RegistryContext.getRegistry().getModels();
        for (Iterator iterator = models.values().iterator(); iterator.hasNext();)
        {
            UMOModel model = (UMOModel) iterator.next();
            if(!model.getName().equals(SYSTEM_MODEL))
            {
                return model;
            }
        }
        return null;
    }
}
