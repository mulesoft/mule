// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.management;

import javax.management.ObjectName;

/**
 * This interface provides methods to create JMX object names for component-
 * supplied MBeans. This ensures that component-supplied MBeans follow the JBI
 * implementation-determined naming convention.
 *
 * @author JSR208 Expert Group
 */
public interface MBeanNames
{
    /**
     * Formulate and return the MBean ObjectName of a custom control MBean for
     * a JBI component.
     *
     * @param customName the name of the custom control.
     * @return the JMX ObjectName of the MBean, or <code>null</code> if 
     * <code>customName</code> is invalid.
     */
    ObjectName createCustomComponentMBeanName(String customName);
    /**
     * The custom name that must be used for bootstrap extensions.
     */
    static final String BOOTSTRAP_EXTENSION = "BootstrapExtension";

    /**
     * The custom name that must be used for component life cycle extensions.
     */
    static final String COMPONENT_LIFE_CYCLE_EXTENSION = "LifeCycleExtension";

    /**
     * Retrieve the default JMX Domain Name for MBeans registered in this
     * instance of the JBI implementation.
     *
     * @return the JMX domain name for this instance of the JBI implementation.
     */
    String getJmxDomainName();
}
