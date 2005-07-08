// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * @(#)file      InstallerMBean.java
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
 * InstallerMBean defines standard installation and uninstallation controls for
 * Binding Components and Service Engines.
 * Binding Components and Service Engines.
 *
 * @author JSR208 Expert Group
 */
public interface InstallerMBean
{
    /**
     * Get the installation root directory path for this BC or SE.
     * @return the full installation path of this component.
     */
    String getInstallRoot();

    /**
     * Install a BC or SE.
     * @return JMX ObjectName representing the ComponentLifeCycle for
     * the installed component, or null if the installation did not complete.
     * @throws javax.jbi.JBIException if the installation fails.
     */
    ObjectName install()
        throws javax.jbi.JBIException;

    /**
     * Determine whether or not the component is installed.
     * @return true if this component is currently installed, false if not.
     */
    boolean isInstalled();

    /**
     * Uninstall a BC or SE. This completely removes the component from the
     * JBI system.
     * @throws javax.jbi.JBIException if the uninstallation fails.
     */
    void uninstall()
        throws javax.jbi.JBIException;

    /**
     * Get the installer configuration MBean name for this component.
     * @return the MBean object name of the Installer Configuration MBean.
     * @throws javax.jbi.JBIException if the component is not in the LOADED state
     * or any error occurs during processing.
     */
    ObjectName getInstallerConfigurationMBean()
        throws javax.jbi.JBIException;
}
