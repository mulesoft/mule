// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * @(#)file      AdminServiceMBean.java
 * @(#)author    Sun Microsystems, Inc.
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.management;

import javax.management.ObjectName;

/**
 * AdminServiceMBean defines the interface that must be implemented by
 * the AdminService in a JBI Framework.
 *
 * @author JSR208 Expert Group
 */
public interface AdminServiceMBean
{
    /**
     * Get a list of all binding components currently installed.
     * @return array of JMX object names of all installed BCs.
     */
    ObjectName[] getBindingComponents();

    /**
     * Lookup a JBI Installable Component by its unique name.
     * @param name - is the name of the BC or SE.
     * @return the JMX object name of the component's LifeCycle MBean or null.
     */
    ObjectName getComponentByName(String name);

    /**
     * Get a list of all engines currently installed.
     * @return array of JMX object names of all installed SEs.
     */
    ObjectName[] getEngineComponents();

    /**
     * Return current version and other info about this JBI Framework.
     * @return info String
     */
    String getSystemInfo();

    /**
     * Lookup a system service by name.
     * @param serviceName - is the name of the system service
     * @return the JMX object name of the service or null
     */
    ObjectName getSystemService(String serviceName);

    /**
     * Looks up all JBI Framework System Services currently installed.
     * @return array of JMX object names of system services
     */
    ObjectName[] getSystemServices();

    /**
     * Check if a given JBI Installable Component is a Binding Component.
     * @param componentName - the unique name of the component
     * @return true if the component is a binding
     */
    boolean isBinding(String componentName);

    /**
     * Check if a given JBI Component is a service engine.
     * @param componentName - the unique name of the component
     * @return true if the component is a service engine
     */
    boolean isEngine(String componentName);
}
