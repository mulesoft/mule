/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model;

import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;

import java.util.Collection;
import java.util.Iterator;

/**
 * @deprecated This functionality should be moved to the registry
 */
public final class ModelHelper
{

    public static final String SYSTEM_MODEL = MuleProperties.OBJECT_SYSTEM_MODEL;
    /** Do not instanciate. */
    private ModelHelper ()
    {
        // no-op
    }

    public static String getSystemModelType()
    {
        return "seda";
    }
    
    public static boolean isComponentRegistered(String name)
    {
        for (Iterator iterator = RegistryContext.getRegistry().getModels().iterator(); iterator.hasNext();)
        {
            UMOModel m = (UMOModel) iterator.next();
            if (m.isComponentRegistered(name))
            {
                return true;
            }
        }
        return false;
    }

    public static UMOComponent getComponent(String name)
    {
        for (Iterator iterator = RegistryContext.getRegistry().getModels().iterator(); iterator.hasNext();)
        {
            UMOModel m = (UMOModel) iterator.next();
            if (m.isComponentRegistered(name))
            {
                return m.getComponent(name);
            }
        }
        return null;
    }

    public static UMODescriptor getDescriptor(String name)
    {
        return RegistryContext.getRegistry().lookupService(name);
//        for (Iterator iterator = RegistryContext.getRegistry().getModels().values().iterator(); iterator.hasNext();)
//        {
//            UMOModel m = (UMOModel) iterator.next();
//            if (m.isComponentRegistered(name))
//            {
//                return m.getDescriptor(name);
//            }
//        }
//        return null;
    }

    //TODO RM*: Move this method
    public static void registerSystemComponent(UMODescriptor d) throws UMOException
    {
        UMOModel model = RegistryContext.getRegistry().lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
        if(model==null)
        {
            model = ModelFactory.createModel("seda");
            model.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
            RegistryContext.getRegistry().registerModel(model);
        }
        d.setModelName(MuleProperties.OBJECT_SYSTEM_MODEL);
        RegistryContext.getRegistry().registerService(d);
    }

    public static UMOModel getFirstUserModel() throws UMOException
    {
        Collection models = RegistryContext.getRegistry().getModels();
        for (Iterator iterator = models.iterator(); iterator.hasNext();)
        {
            UMOModel model = (UMOModel) iterator.next();
            if(!model.getName().equals(MuleProperties.OBJECT_SYSTEM_MODEL))
            {
                return model;
            }
        }
        return null;
    }
}
