/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;

import org.ibeans.impl.IBeansNotationHelper;
import org.ibeans.impl.view.TextView;

/**
 * Holds a reference to an iBeans class in the registry. An iBean instance can be created from this object as well as reporting
 * its usage and short ID.
 */
public class IBeanHolder implements Comparable
{
    public static final String REGISTRY_SUFFIX = ".holder";
    
    private Class ibean;
    private String usage;

    public IBeanHolder(Class ibean)
    {
        this.ibean = ibean;
    }

    public int compareTo(Object o)
    {
        IBeanHolder to = (IBeanHolder) o;
        return getId().compareTo(to.getId());
    }

    public Class getIbeanClass()
    {
        return ibean;
    }

    public Object create(MuleContext muleContext, MuleIBeansPlugin plugin) throws MuleException
    {
        final String name = String.format("%s.%d", ibean.getSimpleName(), System.identityHashCode(this));
        IBeanFlowConstruct flow = new IBeanFlowConstruct(name, muleContext);
        muleContext.getRegistry().registerObject(flow.getName(), flow, FlowConstruct.class);

        IBeanBinding router = new IBeanBinding(flow, muleContext, plugin);
        router.setInterface(ibean);
        return router.createProxy(new Object());
    }

    public String getId()
    {
        return getId(ibean);
    }
    
    public static String getId(Class ibean)
    {
        return IBeansNotationHelper.getIBeanShortID(ibean) + REGISTRY_SUFFIX;
    }
    
    public String getUsage()
    {
        if (usage == null)
        {
            TextView view = new TextView();
            usage = view.createView(ibean);
        }
        return usage;
    }

    @Override
    public String toString()
    {
        return "IBean: " + getId() + " : " + ibean.getName();
    }
}
