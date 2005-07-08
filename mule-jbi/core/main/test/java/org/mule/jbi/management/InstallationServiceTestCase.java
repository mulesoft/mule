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
import org.mule.jbi.util.IOUtils;

public class InstallationServiceTestCase extends TestCase {

	protected JbiContainer container;
	
	public void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
		container = new JbiContainer();
		container.setWorkingDir(new File("target/.mule-jbi"));
		List l = MBeanServerFactory.findMBeanServer(null);
		if (l != null && l.size() > 0) {
			container.setMBeanServer((MBeanServer) l.get(0));
		} else {
			container.setMBeanServer(MBeanServerFactory.createMBeanServer());
		}
		container.start();
	}
	
	public void testInstallComponent() throws Exception {
		Locale.setDefault(Locale.US);
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		ObjectName installON = install.loadNewInstaller(url.toString());
		assertNotNull(installON);
		ObjectName lifecycleON = (ObjectName) container.getMBeanServer().invoke(installON, "install", null, null);
		assertNotNull(lifecycleON);
		container.getMBeanServer().invoke(lifecycleON, "start", null, null);
		container.getMBeanServer().invoke(lifecycleON, "stop", null, null);
	}
	
	public void testInstallComponentWithInfo() throws Exception {
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("component.zip");
		ObjectName result = install.loadNewInstaller(url.toString());
		assertNotNull(result);
	}
	
	public void testInstallBadComponent() throws Exception {
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		ObjectName result = install.loadNewInstaller(url.toString());
		assertNull(result);
	}
	
	public void testInstallSharedLibrary() throws Exception {
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		String result = install.installSharedLibrary(url.toString());
		assertNotNull(result);
	}
	
	public void testInstallBadSharedLibrary() throws Exception {
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		String result = install.installSharedLibrary(url.toString());
		assertNull(result);
	}
	
	public void testInstallUninstallReinstallSharedLibrary() throws Exception {
		InstallationService install = new InstallationService(container);
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		String result = install.installSharedLibrary(url.toString());
		assertNotNull(result);
		assertTrue(install.uninstallSharedLibrary(result));
		result = install.installSharedLibrary(url.toString());
		assertNotNull(result);
	}
	
}
