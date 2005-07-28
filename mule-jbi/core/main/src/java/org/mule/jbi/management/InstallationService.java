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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Library;
import org.mule.jbi.util.IOUtils;

import com.sun.java.xml.ns.jbi.JbiDocument;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class InstallationService implements InstallationServiceMBean {

	public static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";
	
	private static final Log LOGGER = LogFactory.getLog(InstallationService.class);
	
	private Map installers;
	private JbiContainer container;
	
	public InstallationService(JbiContainer container) {
		this.container = container;
		this.installers = new HashMap();
	}
	
    /**
     * Load the installer for a new component from a component installation package.
     *
     * @param installJarURL - URL locating a jar file containing a
     * JBI Installable Component.
     * @return - the JMX ObjectName of the InstallerMBean loaded from
     * installJarURL.
     */
	public synchronized ObjectName loadNewInstaller(String installJarURI) {
		File dir = null;
		try {
			LOGGER.info("Creating new installer for " + installJarURI);
			// Check that the url is valid
			URI uri;
			try {
				uri = new URI(installJarURI);
			} catch (URISyntaxException e) {
				uri = new File(installJarURI).toURI();
			}
			// Create a temporary dir
			dir = Directories.getNewTempDir(this.container.getWorkingDir());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Temporary dir: " + dir);
			}
			IOUtils.createDirs(dir);
			File f = new File(dir, "jbi.zip");
			// Download file
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Copying installation jar to " + f);
			}
			IOUtils.copy(uri.toURL(), f);
			// Unzip to temp dir
			File unzip = new File(dir, "/unzip");
			IOUtils.unzip(f, unzip);
			// Load jbi descriptor
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Loading jbi descriptor");
			}
			File jbiFile = new File(unzip, JBI_DESCRIPTOR);
			if (!jbiFile.isFile()) {
				throw new JBIException("No jbi descriptor found");
			}
			Jbi jbi = JbiDocument.Factory.parse(jbiFile).getJbi();
			// Check version number
			if (jbi.getVersion().doubleValue() != 1.0) {
				throw new JBIException("version attribute should be '1.0'");
			}
			// Check that it is a component
			if (!jbi.isSetComponent()) {
				throw new JBIException("component should be set");
			}
			String name = jbi.getComponent().getIdentification().getName();
			// Check that an installer has not been create already
			if (this.installers.get(name) != null) {
				throw new JBIException("an installer has already been created");
			}
			// Check that component does not already exists
			if (this.container.getRegistry().getComponent(name) != null) {
				throw new JBIException("component already installed");
			}
			// Retrieve component type
			boolean engine = jbi.getComponent().getType() == com.sun.java.xml.ns.jbi.ComponentDocument.Component.Type.SERVICE_ENGINE; 
			// Init directories
			File installDir;
			File workspaceDir;
			if (engine) {
				installDir = Directories.getEngineInstallDir(this.container.getWorkingDir(), name);
				workspaceDir = Directories.getEngineWorkspaceDir(this.container.getWorkingDir(), name);
			} else {
				installDir = Directories.getBindingInstallDir(this.container.getWorkingDir(), name);
				workspaceDir = Directories.getBindingWorkspaceDir(this.container.getWorkingDir(), name);
			}
			IOUtils.deleteFile(installDir);
			IOUtils.deleteFile(workspaceDir);
			IOUtils.createDirs(installDir);
			IOUtils.createDirs(workspaceDir);
			// Unzip all
			IOUtils.unzip(f, installDir);
			// Create component
			Component component;
			if (engine) {
				component = this.container.getRegistry().addEngine(name);
			} else {
				component = this.container.getRegistry().addBinding(name);
			}
			component.setInstallRoot(installDir.getAbsolutePath());
			component.setWorkspaceRoot(workspaceDir.getAbsolutePath());
			component.setDescriptor(jbi);
			// Create and register installer
			Installer installer = new Installer(this.container, component);
			installer.init();
			ObjectName objName = createComponentInstallerName(name);
			StandardMBean mbean = new StandardMBean(installer, InstallerMBean.class);
			this.container.getMBeanServer().registerMBean(mbean, objName);
			this.installers.put(name, installer);
			return objName;
		} catch (Exception e) {
			LOGGER.error("Could not create installer", e);
			throw new RuntimeException("Could not create installer", e);
		} finally {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Deleting temporary dir: " + dir);
			}
			IOUtils.deleteFile(dir);
		}
	}

	private ObjectName createComponentInstallerName(String name) {
		return this.container.createMBeanName(name, "installer", null);
	}

	public synchronized ObjectName loadInstaller(String aComponentName) {
		try {
			if (this.installers.get(aComponentName) != null) {
				return createComponentInstallerName(aComponentName);
			} else {
				Component component = this.container.getRegistry().getComponent(aComponentName);
				if (component == null) {
					throw new JBIException("Component not installed: " + aComponentName);
				}
				Installer installer = new Installer(this.container, component);
				installer.init();
				ObjectName objName = createComponentInstallerName(aComponentName);
				StandardMBean mbean = new StandardMBean(installer, InstallerMBean.class);
				this.container.getMBeanServer().registerMBean(mbean, objName);
				this.installers.put(aComponentName, installer);
				return objName;
			}
		} catch (Exception e) {
			LOGGER.error("Could not create installer", e);
			throw new RuntimeException("Could not create installer", e);
		}
	}

	public synchronized boolean unloadInstaller(String aComponentName, boolean isToBeDeleted) {
		Installer installer = (Installer) this.installers.get(aComponentName);
		if (installer == null) {
			return false;
		}
		try {
			Component component = this.container.getRegistry().getComponent(aComponentName);
			if (component != null && component.getCurrentState().equals(Component.UNKNOWN)) {
				component.uninstall();
			}
			ObjectName objName = createComponentInstallerName(aComponentName);
			if (this.container.getMBeanServer().isRegistered(objName)) {
				this.container.getMBeanServer().unregisterMBean(objName);
			}
			this.installers.remove(aComponentName);
			return true;
		} catch (Exception e) {
			LOGGER.info("unloadInstaller", e);
			return false;
		}
	}

	public synchronized String installSharedLibrary(String aSharedLibURI) {
		File dir = null;
		try {
			// Check that the url is valid
			URI uri = new URI(aSharedLibURI);
			// Create a temporary dir
			dir = Directories.getNewTempDir(this.container.getWorkingDir());
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
		// Load jbi descriptor
		URL jbiUrl = new URL("jar", "", file.toURL().toString() + "!/" + JBI_DESCRIPTOR);
		Jbi jbi = JbiDocument.Factory.parse(jbiUrl).getJbi();
		// Check version number
		if (jbi.getVersion().doubleValue() != 1.0) {
			throw new JBIException("version attribute should be '1.0'");
		}
		// Check that it is a component
		if (!jbi.isSetSharedLibrary()) {
			throw new JBIException("shared-library should be set");
		}
		// Retrieve name
		String name = jbi.getSharedLibrary().getIdentification().getName();
		// Check that it is not already installed
		if (this.container.getRegistry().getLibrary(name) != null) {
			throw new JBIException("Shared library is already installed");
		}
		// Create library
		File installDir = Directories.getLibraryInstallDir(this.container.getWorkingDir(), name);
		Library lib = this.container.getRegistry().addLibrary(name);
		lib.setInstallRoot(installDir.getAbsolutePath());
		// Move unzipped files to install dir
		IOUtils.unzip(file, installDir);
		// Finish installation
		lib.install();
		return name;
	}

	public synchronized boolean uninstallSharedLibrary(String aSharedLibName) {
		try {
			Library lib = this.container.getRegistry().getLibrary(aSharedLibName);
			if (lib == null) {
				throw new JBIException("Library does not exists"); 
			}
			lib.uninstall();
			return true;
		} catch (Exception e) {
			LOGGER.error("Error uninstalling library", e);
			return false;
		}
	}

}
