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
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.management.InstallationServiceMBean;
import javax.jbi.management.InstallerMBean;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.util.IOUtils;

import com.sun.java.xml.ns.jbi.JbiDocument;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

public class InstallationService implements InstallationServiceMBean {

	public static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";
	public static final String TMP_DIR = "/tmp";
	public static final String CMP_DIR = "/cmp";
	public static final String LIB_DIR = "/lib";
	
	private static final Log LOGGER = LogFactory.getLog(InstallationService.class);
	
	private JbiContainer container;
	private Map installers;
	private File tmpDir;
	private File cmpDir;
	private File libDir;
	private int counter;
	
	public InstallationService(JbiContainer container) {
		this.container = container;
		this.installers = new HashMap();
		this.tmpDir = new File(container.getWorkingDir(), TMP_DIR);
		this.tmpDir.mkdirs();
		this.cmpDir = new File(container.getWorkingDir(), CMP_DIR);
		this.cmpDir.mkdirs();
		this.libDir = new File(container.getWorkingDir(), LIB_DIR);
		this.libDir.mkdirs();
		this.counter = 0;
	}
	
	private File getTmpDir() {
		while (true) {
			String s = Integer.toHexString(++counter);
			while (s.length() < 8) {
				s = "0" + s;
			}
			File f = new File(this.tmpDir, s);
			if (!f.exists()) {
				return f;
			}
		}
	}
	
	public synchronized ObjectName loadNewInstaller(String installJarURI) {
		File dir = null;
		try {
			// Check that the url is valid
			URI uri = new URI(installJarURI);
			// Create a temporary dir
			dir = getTmpDir();
			IOUtils.createDirs(dir);
			File f = new File(dir, "jbi.zip");
			// Download file
			IOUtils.copy(uri.toURL(), f);
			// Install from the downloaded file
			return loadNewInstaller(f, dir);
		} catch (Exception e) {
			RuntimeException re = new RuntimeException("Could not install component", e); 
			LOGGER.error(re);
			throw re;
		} finally {
			IOUtils.deleteFile(dir);
		}
	}

	private ObjectName loadNewInstaller(File file, File dir) throws Exception {
		// Unzip file to temp dir
		File unzip = new File(dir, "unzip");
		IOUtils.unzip(file, unzip);
		// Load jbi descriptor
		File jbiFile = new File(unzip, JBI_DESCRIPTOR);
		if (!jbiFile.isFile()) {
			throw new JBIException("Jar has no jbi.xml");
		}
		Jbi jbi = JbiDocument.Factory.parse(jbiFile).getJbi();
		// Check version number
		if (!"1.0".equals(jbi.getVersion().toString())) {
			throw new JBIException("version attribute should be '1.0'");
		}
		// Check that it is a component
		if (!jbi.isSetComponent()) {
			throw new JBIException("component should be set");
		}
		// TODO: check libs
		String name = jbi.getComponent().getIdentification().getName();
		// Check that component does not already exists
		if (this.container.getComponentRegistry().getComponent(name) != null) {
			throw new JBIException("component already installed");
		}
		// Check that an installer has not been create already
		if (this.installers.get(name) != null) {
			throw new JBIException("an installer has already been created");
		}
		// Move unzipped files to install dir
		File installDir = new File(cmpDir, name);
		// TODO: unzip should be part of installation
		if (!unzip.renameTo(installDir)) {
			throw new IOException("Could not rename directory: " + unzip);
		}
		// Create and register installer
		Installer installer = new Installer(this.container, installDir, jbi);
		installer.init();
		ObjectName objName = createComponentInstallerName(name);
		StandardMBean mbean = new StandardMBean(installer, InstallerMBean.class);
		this.container.getMBeanServer().registerMBean(mbean, objName);
		this.installers.put(name, installer);
		return objName;
	}

	private ObjectName createComponentInstallerName(String name) {
		return this.container.createComponentMBeanName(name, "installer", null);
	}

	public synchronized ObjectName loadInstaller(String aComponentName) {
		if (this.installers.get(aComponentName) != null) {
			return createComponentInstallerName(aComponentName);
		} else {
			return null;
		}
	}

	public synchronized boolean unloadInstaller(String aComponentName, boolean isToBeDeleted) {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized String installSharedLibrary(String aSharedLibURI) {
		File dir = null;
		try {
			// Check that the url is valid
			URI uri = new URI(aSharedLibURI);
			// Create a temporary dir
			dir = getTmpDir();
			IOUtils.createDirs(dir);
			File f = new File(dir, "jbi.zip");
			// Download file
			IOUtils.copy(uri.toURL(), f);
			// Install from the downloaded file
			return installSharedLibrary(f, dir);
		} catch (Exception e) {
			LOGGER.error("Could not install shared library", e);
			return null;
		} finally {
			IOUtils.deleteFile(dir);
		}
	}

	private String installSharedLibrary(File file, File dir) throws Exception {
		// Unzip file to temp dir
		File unzip = new File(dir, "unzip");
		IOUtils.unzip(file, unzip);
		// Load jbi descriptor
		File jbiFile = new File(unzip, JBI_DESCRIPTOR);
		Jbi jbi = JbiDocument.Factory.parse(jbiFile).getJbi();
		// Check version number
		if (!"1.0".equals(jbi.getVersion().toString())) {
			throw new JBIException("version attribute should be '1.0'");
		}
		// Check that it is a component
		if (!jbi.isSetSharedLibrary()) {
			throw new JBIException("shared-library should be set");
		}
		// Move unzipped files to install dir
		String name = jbi.getSharedLibrary().getIdentification().getName();
		// Check that it is not already installed
		if (this.container.getComponentRegistry().getSharedLibrary(name) != null) {
			throw new JBIException("Shared library is already installed");
		}
		File installDir = new File(libDir, name);
		if (!unzip.renameTo(installDir)) {
			throw new IOException("Could not rename directory: " + unzip);
		}
		this.container.getComponentRegistry().registerSharedLibrary(name);
		return name;
	}

	public synchronized boolean uninstallSharedLibrary(String aSharedLibName) {
		try {
			this.container.getComponentRegistry().unregisterSharedLibrary(aSharedLibName);
			File installDir = new File(libDir, aSharedLibName);
			IOUtils.deleteFile(installDir);
			return true;
		} catch (JBIException e) {
			LOGGER.error("Error uninstalling library", e);
			return false;
		}
	}

}
