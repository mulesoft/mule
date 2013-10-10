/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Support class for JMX 1.1 based systems.
 */
public class JmxLegacySupport extends AbstractJmxSupport
{

    /**
     * Uses simpler rules for escaping non-JMX compliant chars.
     * Much of the work has already been performed in {@link org.mule.util.ObjectNameHelper}.
     *
     * @param name value to escape for JMX compliance
     * @return value valid for JMX
     */
    public String escape(String name)
    {
        // do nothing at the moment, as ObjectNameHelper handles most of the conversion scenarios
        // kept as a placeholder and no-op to keep newer JMX classes from kicking in.
        return name;
    }


    /**
     * For modern JMX implementation just delegate to a standard factory method.
     *
     * @param name object name
     * @return ObjectName for MBeanServer
     * @throws javax.management.MalformedObjectNameException
     *          for invalid names
     */
    public ObjectName getObjectName(String name) throws MalformedObjectNameException
    {
        return new ObjectName(name);
    }


    /** {@inheritDoc} */
    protected Collection getDomains(final MBeanServer server)
    {
        // list all MBean names and collect unique domains
        Set set = server.queryNames(null, null);
        Set domains = new HashSet();
        for (Iterator it = set.iterator(); it.hasNext();)
        {
            ObjectName objectName = (ObjectName) it.next();
            domains.add(objectName.getDomain());
        }
        return domains;
    }
}
