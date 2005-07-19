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

import java.io.File;
import java.net.URL;
import java.util.Locale;

import javax.management.ObjectName;

import junit.framework.TestCase;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.util.IOUtils;

public class FunctionalTestCase extends TestCase {

	protected JbiContainer container;
	
	protected void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		jbi.start();
		container = jbi;
	}
	
	public void testRealInstall() throws Exception {
		Locale.setDefault(Locale.US);
		installLibrary("wsdlsl.jar");
		installComponent("filebinding.jar");
		container.shutDown();
		container.start();
		installComponent("transformationengine.jar");
		deployAssembly("sa.zip");
		installComponent("soapbinding.jar");
		installComponent("sequencingengine.jar");
	}
	
	protected void installLibrary(String file) throws Exception {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource(file);
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(result);
	}
	
	protected void installComponent(String file) throws Exception {
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		URL url = Thread.currentThread().getContextClassLoader().getResource(file);
		ObjectName installON = (ObjectName) container.getMBeanServer().invoke(serviceON, "loadNewInstaller", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(installON);
		ObjectName lifecycleON = (ObjectName) container.getMBeanServer().invoke(installON, "install", null, null);
		assertNotNull(lifecycleON);
		container.getMBeanServer().invoke(lifecycleON, "start", null, null);
	}
	
	protected void deployAssembly(String file) throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource(file);
		ObjectName serviceON = container.createMBeanName(null, "service", "deploy");
		String result = (String) container.getMBeanServer().invoke(serviceON, "deploy", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		System.err.println(result);
	}
	
}
