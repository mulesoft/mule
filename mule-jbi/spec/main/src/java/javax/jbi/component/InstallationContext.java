// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * InstallationContext.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.component;

import org.w3c.dom.DocumentFragment;

/**
 * This context contains information necessary for a JBI component to
 * perform its installation/uninstallation processing. This is provided to
 * the init() method of the component {@link Bootstrap} interface. 
 *
 * @author JSR208 Expert Group
 */
public interface InstallationContext
{
    /**
     * Get the name of the class that implements the {@link Component}
     * interface for this component. This must be the component
     * class name given in the component's installation descriptor.
     * 
     * @return the {@link Component} implementation class name, which must be
     *         non-null and non-empty.
     */
    String getComponentClassName();

    /**
     * Get a list of elements that comprise the class path for this component. 
     * Each element represents either a directory (containing class files) or a 
     * library file. All elements are reachable from the install root. These
     * elements represent class path items that the component's execution-time 
     * component class loader uses, in search order. All path elements must
     * use the file separator character appropriate to the system (i.e.,
     * <code>File.separator</code>).
     * 
     * @return a list of String objects, each of which contains a class path 
     *         elements. The list must contain at least one class path element.
     */
    java.util.List getClassPathElements();

    /**
     * Get the unique name assigned to this component. This name must be 
     * assigned from the component's installation descriptor identification 
     * section.
     * 
     * @return the unique component name, which must be non-null and non-empty.
     */
    String getComponentName();

    /**
     * Get the JBI context for this component. The following methods are
     * valid to use on the context:
     * <ul>
     *   <li>{@link ComponentContext#getMBeanNames()}</li>
     *   <li>{@link ComponentContext#getMBeanServer()}</li>
     *   <li>{@link ComponentContext#getNamingContext()}</li>
     *   <li>{@link ComponentContext#getTransactionManager()}</li>
     * </ul>
     * All other methods on the returned context must throw a
     * <code>IllegalStateException</code> exception if invoked.
     * 
     * @return the JBI context for this component, which must be non-null.
     */
    ComponentContext getContext();

    /**
     * Get the installation root directory full path name for this component.
     * This path name must be formatted for the platform the JBI environment
     * is running on.
     * 
     * @return the installation root directory name, which must be non-null and
     *         non-empty.
     */
    String getInstallRoot();

    /**
     * Return a DOM document fragment representing the installation descriptor 
     * (jbi.xml) extension data for the component, if any.
     * <p>
     * The Installation Descriptor Extension data are located at the end of the
     * &lt;component&gt; element of the installation descriptor.
     * 
     * @return a DOM document fragment containing the installation descriptor 
     * (jbi.xml) extension data, or <code>null</code> if none is present in the 
     * descriptor.
     */
    DocumentFragment getInstallationDescriptorExtension();

    /**
     * Returns <code>true</code> if this context was created in order to install
     * a component into the JBI environment. Returns <code>false</code> if this
     * context was created to uninstall a previously installed component.
     * <p>
     * This method is provided to allow {@link Bootstrap} implementations to
     * tailor their behaviour according to use case. For example, the
     * {@link Bootstrap#init(InstallationContext)} method implementation may 
     * create different types of extension MBeans, depending on the use case 
     * specified by this method.
     * 
     * @return <code>true</code> if this context was created in order to install
     *         a component into the JBI environment; otherwise the context
     *         was created to uninstall an existing component.
     */
    boolean isInstall();

    /**
     * Set the list of elements that comprise the class path for this component.
     * Each element represents either a directory (containing class files) or a 
     * library file. Elements are reached from the install root. These
     * elements represent class path items that the component's execution-time 
     * component class loader uses, in search order. All file paths are 
     * relative to the install root of the component.
     * <p>
     * This method allows the component's bootstrap to alter the execution-time
     * class path specified by the component's installation descriptor. The 
     * component configuration determined during installation can affect the 
     * class path needed by the component at execution-time. All path elements
     * must use the file separator character appropriate to the system (i.e.,
     * <code>File.separator</code>.
     * 
     * @param classPathElements a list of String objects, each of which contains
     *        a class path elements; the list must be non-null and contain at 
     *        least one class path element.
     * @exception IllegalArgumentException if the class path elements is null, 
     *            empty, or if an individual element is ill-formed.
     */
    void setClassPathElements(java.util.List classPathElements);
}
