/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

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
