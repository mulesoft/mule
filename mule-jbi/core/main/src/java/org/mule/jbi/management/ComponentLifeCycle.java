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

import java.io.IOException;

import javax.jbi.JBIException;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.management.ObjectName;

public class ComponentLifeCycle implements ComponentLifeCycleMBean {

	private javax.jbi.component.ComponentLifeCycle lifeCycle;
	
	public ComponentLifeCycle(javax.jbi.component.ComponentLifeCycle lifeCycle) {
		this.lifeCycle = lifeCycle;
	}
	
	public ObjectName getExtensionMBeanName() throws JBIException {
		// TODO Auto-generated method stub
		return this.lifeCycle.getExtensionMBeanName();
	}

	public void start() throws JBIException, IOException {
		this.lifeCycle.start();
	}

	public void stop() throws JBIException, IOException {
		this.lifeCycle.stop();
	}

	public void shutDown() throws JBIException, IOException {
		this.lifeCycle.shutDown();
	}

	public String getCurrentState() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
