/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.jbi.registry;

import com.sun.java.xml.ns.jbi.JbiDocument;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.messaging.DeliveryChannelImpl;
import org.mule.registry.ComponentType;
import org.mule.registry.Library;
import org.mule.registry.Registry;
import org.mule.registry.RegistryException;
import org.mule.registry.Unit;
import org.mule.registry.impl.AbstractRegistryComponent;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.jbi.messaging.DeliveryChannel;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.IOException;
import java.util.Arrays;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiRegistryComponent  extends AbstractRegistryComponent  {

    protected DeliveryChannel channel;
    protected JbiContainer container;

    public JbiRegistryComponent(String name, ComponentType type, Registry registry) {
        super(name, type, registry);
        container = JbiContainer.Factory.getInstance();
    }




	public synchronized ObjectName initComponent() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN) && !getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.component.getClass().getClassLoader());
			this.objectName = container.createMBeanName(getName(), "lifecycle", null);
			this.channel = new DeliveryChannelImpl(container, getName());
			org.mule.jbi.management.ComponentLifeCycle lf = new org.mule.jbi.management.ComponentLifeCycle(container.getManagementContext(), this);
			lf.init();
			if (container.getMBeanServer().isRegistered(this.objectName)) {
				container.getMBeanServer().unregisterMBean(this.objectName);
			}
			container.getMBeanServer().registerMBean(new StandardMBean(lf, ComponentLifeCycleMBean.class), objectName);
			setCurrentState(INITIALIZED);
			return objectName;
		} catch (Exception e) {
			throw new JBIException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#install()
	 */
	public void doInstall() throws Exception {
		JbiDocument.Jbi jbi = ((JbiDocument.Jbi)getDescriptor().getConfiguration());
		// Check shared libraries
		com.sun.java.xml.ns.jbi.ComponentDocument.Component.SharedLibrary[] libs = jbi.getComponent().getSharedLibraryArray();
		for (int i = 0; i < libs.length; i++) {
			String libName = libs[i].getDomNode().getFirstChild().getNodeValue();
			Library library = getRegistry().getLibrary(libName);
			if (library == null) {
				throw new RegistryException("Component requires a missing shared library: " + libName);
			}
			library.addComponent(this);
		}

        // Get class path elements
		this.classPathElements = Arrays.asList(jbi.getComponent().getComponentClassPath().getPathElementArray());
		// Class loader delegation
		this.isClassLoaderParentFirst = !com.sun.java.xml.ns.jbi.ComponentDocument.Component.ComponentClassLoaderDelegation.SELF_FIRST.equals(jbi.getComponent().getComponentClassLoaderDelegation());
		// Get component class name
		this.componentClassName = jbi.getComponent().getComponentClassName().getDomNode().getFirstChild().getNodeValue();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#restoreState(org.mule.jbi.JbiContainer)
	 */
	public synchronized void doRestoreState() throws Exception {

		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			((Component)getComponent()).getServiceUnitManager().deploy(units[i].getName(), units[i].getInstallRoot());
			if (units[i].getStateAtShutdown().equals(RUNNING)) {
				units[i].start();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#start()
	 */
	public void doStart() throws JBIException, IOException {
		((Component)component).getLifeCycle().start();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#stop()
	 */
	public void doStop() throws JBIException, IOException {
		((Component)component).getLifeCycle().stop();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#shutDown()
	 */
	public void doShutDown() throws JBIException, IOException {
		// TODO: unregister mbean
		((Component)component).getLifeCycle().shutDown();
	}


	public DeliveryChannel getChannel() {
		return this.channel;
	}
}