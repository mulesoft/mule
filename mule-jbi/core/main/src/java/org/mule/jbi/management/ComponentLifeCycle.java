/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.management;

import org.mule.ManagementContext;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.ComponentContextImpl;
import org.mule.jbi.registry.JbiRegistryComponent;
import org.mule.registry.RegistryException;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.management.ObjectName;
import java.io.IOException;

/**
 * Management bean for a component lifecycle.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class ComponentLifeCycle implements ComponentLifeCycleMBean {

	private ManagementContext context;
	private JbiRegistryComponent component;
	
	public ComponentLifeCycle(ManagementContext context, JbiRegistryComponent component) {
		this.context = context;
		this.component = component;
	}
	
	public synchronized void init() throws JBIException {
		ComponentContextImpl context = new ComponentContextImpl(this.context, this.component,
                JbiContainer.Factory.getInstance().getEndpoints());
		((Component)component.getComponent()).getLifeCycle().init(context);
	}
	
	/* (non-Javadoc)
	 * @see javax.jbi.management.ComponentLifeCycleMBean#getExtensionMBeanName()
	 */
	public ObjectName getExtensionMBeanName() throws JBIException {
		return ((Component)component.getComponent()).getLifeCycle().getExtensionMBeanName();
	}

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#start()
	 */
	public void start() throws JBIException, IOException {
        try {
            this.component.start();
        } catch (RegistryException e) {
            throw new JBIException(e);
        }
    }

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#stop()
	 */
	public void stop() throws JBIException, IOException {
        try {
            this.component.stop();
        } catch (RegistryException e) {
            throw new JBIException(e);
        }
    }

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#shutDown()
	 */
	public void shutDown() throws JBIException, IOException {
        try {
            this.component.shutDown();
        } catch (RegistryException e) {
            throw new JBIException(e);
        }
    }

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#getCurrentState()
	 */
	public String getCurrentState() throws IOException {
		return this.component.getCurrentState();
	}

}
