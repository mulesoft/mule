// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * @(#)file      ComponentLifeCycleMBean.java
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
 * ComponentLifeCycleMBean defines standard lifecycle controls for JBI
 * Installable Components, and adds a getter for the optional extension
 * MBean name.
 *
 * @author JSR208 Expert Group
 */
public interface ComponentLifeCycleMBean extends LifeCycleMBean
{
    /**
     * Get the JMX ObjectName for any additional MBean for this component. If
     * there is none, return null.
     * @throws javax.jbi.JBIException if there is a failure getting the MBean
     * object name.
     * @return ObjectName the JMX object name of the additional MBean or null
     * if there is no additional MBean.
     */
    ObjectName getExtensionMBeanName() throws javax.jbi.JBIException;
}
