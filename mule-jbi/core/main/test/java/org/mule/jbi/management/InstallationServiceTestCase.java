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

import junit.framework.TestCase;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.AbstractFunctionalTestCase;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.util.IOUtils;

import javax.management.ObjectName;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.io.File;
import java.net.URL;
import java.util.Locale;

public class InstallationServiceTestCase extends AbstractFunctionalTestCase {

	
	public void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		jbi.initialize();
		jbi.start();
		container = jbi;
	}

    public void testInstallSharedLibrary() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNotNull(result);
	}

    public void testAlreadyInstalledSharedLibrary() throws Exception {
		installLibrary("wsdlsl.jar");
        URL url = Thread.currentThread().getContextClassLoader().getResource("wsdlsl.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNull(result);
	}

	public void testInstallBadSharedLibrary() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		ObjectName serviceON = container.createMBeanName(null, "service", "install");
		String result = (String) container.getMBeanServer().invoke(serviceON, "installSharedLibrary", new Object[] { url.toString() }, new String[] { "java.lang.String" });
		assertNull(result);
	}

	public void testInstallComponent() throws Exception {
        installLibrary("wsdlsl.jar");
		Locale.setDefault(Locale.US);
		URL url = Thread.currentThread().getContextClassLoader().getResource("filebinding.jar");
		assertNotNull(url);
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
	
}
