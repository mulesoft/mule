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
 */
package org.mule.jbi.management;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.routing.RouterImpl;
import org.mule.jbi.util.IOUtils;

public class InstallationServiceTestCase extends TestCase {

	protected JbiContainer container;
	
	public void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		jbi.initialize();
		jbi.start();
		container = jbi;
	}
	
	public void testInstallComponent() throws Exception {
		Locale.setDefault(Locale.US);
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		ObjectName installON = (ObjectName) container.getMBeanServer().invoke(serviceON, "loadNewInstaller", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(installON);
		ObjectName lifecycleON = (ObjectName) container.getMBeanServer().invoke(installON, "install", null, null);
		assertNotNull(lifecycleON);
		container.getMBeanServer().invoke(lifecycleON, "start", null, null);
		container.getMBeanServer().invoke(lifecycleON, "stop", null, null);
	}
	
	public void testInstallComponentWithInfo() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("component.zip");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		ObjectName result = (ObjectName) container.getMBeanServer().invoke(serviceON, "loadNewInstaller", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(result);
	}
	
	public void testInstallBadComponent() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		ObjectName result = (ObjectName) container.getMBeanServer().invoke(serviceON, "loadNewInstaller", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNull(result);
	}
	
	public void testInstallSharedLibrary() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(result);
	}
	
	public void testInstallBadSharedLibrary() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNull(result);
	}
	
}
