/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
