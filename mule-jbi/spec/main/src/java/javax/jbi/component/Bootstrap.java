// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * Bootstrap.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.component;

import javax.jbi.JBIException;

import javax.management.ObjectName;

/**
 * This interface is implemented by a JBI Component to provide any special 
 * processing required at install/uninstall time. The methods defined here are 
 * called by the JBI implementation during the installation (or uninstallation)
 * of the component that, among other things, supplies an implementation of this
 * interface. 
 * <p>
 * Initialization/cleanup tasks such as creation/deletion of directories, files,
 * and database tables can be done by the {@link #onInstall()} and {@link 
 * #onUninstall()} methods, respectively. This also allows the component to 
 * terminate the installation or uninstallation in the event of an error.
 * <p>
 * After calling {@link #onInstall()} or {@link #onUninstall()}, regardless of
 * outcome, the JBI implementation must call the {@link #cleanUp()} method
 * afterwards. Similarly, if {@link #init(InstallationContext)} fails with
 * an exception, the JBI implementation must call the {@link #cleanUp()} method.
 * <p>
 * Component implementors should note that there is no guarantee that 
 * the same instance of its <code>Bootstrap</code> implementation will be used
 * during both install and uninstall operations on the component. Data that
 * need to be retained between installation-time and uninstallation-time 
 * must be persisted in such as fashion that a separate instance of the
 * bootstrap class can find them, despite component or system shutdown.
 *
 * @author JSR208 Expert Group
 */
public interface Bootstrap
{
    /**
     * Initializes the installation environment for a component. This method is
     * expected to save any information from the installation context that
     * may be needed by other methods.
     * <p>
     * If the component needs to register an optional installer configuration
     * MBean, it MUST do so during execution of this method, or the
     * getExtensionMBean() method.
     * <p>
     * This method must be called after the installation root (available
     * through the installContext parameter) is prepared.
     * 
     * @param installContext the context containing information from the
     *                       install command and from the component installation
     *                       ZIP file; this must be non-null.
     * @exception JBIException when there is an error requiring that
     *            the installation be terminated
     */
    void init(InstallationContext installContext)
        throws JBIException;

    /**
     * Cleans up any resources allocated by the bootstrap implementation,
     * including performing deregistration of the extension MBean, if 
     * applicable.
     * <p>
     * This method must be called after the onInstall() or onUninstall() method
     * is called, whether it succeeds or fails. It must be called after
     * init() is called, if init() fails by throwing an exception.
     * 
     * @exception JBIException if the bootstrap cannot clean up allocated
     *            resources
     */
    void cleanUp() throws JBIException;

    /**
     * Obtains the <code>ObjectName</code> of the optional installer 
     * configuration MBean. If none is provided by this component, this method 
     * must return <code>null</code>.
     * <p>
     * This method must be called before onInstall() (or onUninstall()) is
     * called by the JBI implementation.
     * 
     * @return ObjectName of the optional installer configuration MBean; 
     *         returns <code>null</code> if there is no such MBean
     */
    ObjectName getExtensionMBeanName();

    /**
     * Called at the beginning of installation of a component to perform any 
     * special installation tasks required by the component. 
     * <p>
     * This method must not be called if the init() method failed with an
     * exception.
     * 
     * @exception JBIException when there is an error requiring that
     *            the installation be terminated
     */
    void onInstall()
        throws JBIException;

    /**
     * Called at the beginning of uninstallation of a component to perform any
     * special uninstallation tasks required by the component.
     * <p>
     * This method must not be called if the init() method failed with an
     * exception.
     * 
     * @exception JBIException when there is an error requiring that
     *            the uninstallation be terminated.
     */
    void onUninstall()
        throws JBIException;
}
