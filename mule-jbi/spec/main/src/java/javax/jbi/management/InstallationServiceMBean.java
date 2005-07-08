// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * @(#)file      InstallationServiceMBean.java
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
 * The InstallationService is responsible for the installation and
 * uninstallation of SEs, BCs, and JBI Framework shared libraries.
 *
 * @author JSR208 Expert Group
 */
public interface InstallationServiceMBean
{
    /**
     * Load the installer for a new component from a component installation package.
     *
     * @param installJarURL - URL locating a jar file containing a
     * JBI Installable Component.
     * @return - the JMX ObjectName of the InstallerMBean loaded from
     * installJarURL.
     */
    ObjectName loadNewInstaller(String installJarURL);

    /**
     * Load the InstallerMBean for a previously installed component.
     *
     * @param aComponentName - the component name identifying the installer to load.
     * @return - the JMX ObjectName of the InstallerMBean loaded from an existing
     * installation context.
     */
    ObjectName loadInstaller(String aComponentName);

    /**
     * Unload a JBI Installable Component installer.
     *
     * @param aComponentName - the component name identifying the installer to unload.
     * @param isToBeDeleted - true iff the component is to be deleted as well.
     * @return - true if the operation was successful, otherwise false.
     */
    boolean
    unloadInstaller(String aComponentName, boolean isToBeDeleted);

    /**
     * Install a shared library jar.
     *
     * @param aSharedLibURI - URI locating a jar file containing a shared library.
     * @return - the name of the shared library loaded from aSharedLibURI.
     */
    String installSharedLibrary(String aSharedLibURI);

    /**
     * Uninstall a shared library.
     *
     * @param aSharedLibName - the name of the shared library to uninstall.
     * @return - true iff the uninstall was successful.
     */
    boolean uninstallSharedLibrary(String aSharedLibName);
}
