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
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.jbi.JBIException;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.management.DeploymentServiceMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.registry.Assembly;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Registry;
import org.mule.jbi.registry.Unit;
import org.mule.jbi.util.IOUtils;

import com.sun.java.xml.ns.jbi.JbiDocument;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class DeploymentService implements DeploymentServiceMBean {

	private static final Log LOGGER = LogFactory.getLog(DeploymentService.class);
	
	public static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";
	
	private JbiContainer container;
	private Registry registry;
	
	public DeploymentService(JbiContainer container) {
		this.container = container;
		this.registry = container.getRegistry();
	}
	
	public synchronized String deploy(String saZipURL) throws Exception {
		File dir = null;
		try {
			// Check that the url is valid
			URI uri = new URI(saZipURL);
			// Create a temporary dir
			dir = Directories.getNewTempDir(this.container.getWorkingDir());
			IOUtils.createDirs(dir);
			File archive = new File(dir, "jbi.zip");
			// Download file
			IOUtils.copy(uri.toURL(), archive);
			// Install from the downloaded file
			// Unzip file to temp dir
			File unzip = new File(dir, "unzip");
			IOUtils.unzip(archive, unzip);
			// Load jbi descriptor
			File jbiFile = new File(unzip, JBI_DESCRIPTOR);
			Jbi jbi = JbiDocument.Factory.parse(jbiFile).getJbi();
			// Check version number
			if (jbi.getVersion().doubleValue() != 1.0) {
				throw new JBIException("version attribute should be '1.0'");
			}
			// Check that it is a component
			if (!jbi.isSetServiceAssembly()) {
				throw new JBIException("service-assembly should be set");
			}
			// Move unzipped files to install dir
			String name = jbi.getServiceAssembly().getIdentification().getName();
			// Check that it is not already installed
			if (this.registry.getAssembly(name) != null) {
				throw new JBIException("Service assembly is already installed");
			}
			File installDir = Directories.getAssemblyInstallDir(this.container.getWorkingDir(), name);
			if (!unzip.renameTo(installDir)) {
				throw new IOException("Could not rename directory: " + unzip);
			}
			// Create assembly
			Assembly assembly = this.registry.addAssembly(name);
			assembly.setDescriptor(jbi);
			assembly.setInstallRoot(installDir.getAbsolutePath());
			String result = assembly.deploy();
			return result;
		} catch (Exception e) {
			LOGGER.error("Could not install shared library", e);
			throw new RuntimeException("Could not install shared library", e);
		} finally {
			IOUtils.deleteFile(dir);
		}
	}

	public String undeploy(String saName) throws Exception {
		Assembly assembly = this.registry.getAssembly(saName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + saName);
		}
		return assembly.undeploy();
	}

	public String[] getDeployedServiceUnitList(String componentName)
			throws Exception {
		Component component = this.registry.getComponent(componentName);
		if (component == null) {
			throw new JBIException("Component not installed: " + componentName);
		}
		Unit[] units = component.getUnits();
		String[] names = new String[units.length];
		for (int i = 0; i < units.length; i++) {
			names[i] = units[i].getName();
		}
		return names;
	}

	public String[] getDeployedServiceAssemblies() throws Exception {
		Assembly[] assemblies = this.registry.getAssemblies();
		String[] names = new String[assemblies.length];
		for (int i = 0; i < assemblies.length; i++) {
			names[i] = assemblies[i].getName();
		}
		return names;
	}

	public String getServiceAssemblyDescriptor(String saName) throws Exception {
		Assembly assembly = this.registry.getAssembly(saName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + saName);
		}
		return assembly.getDescriptor().xmlText();
	}

	public String[] getDeployedServiceAssembliesForComponent(
			String componentName) throws Exception {
		Component component = this.registry.getComponent(componentName);
		if (component == null) {
			throw new JBIException("Component not installed: " + componentName);
		}
		if (component != null) {
			Unit[] units = component.getUnits();
			Set names = new HashSet();
			for (int i = 0; i < units.length; i++) {
				names.add(units[i].getAssembly().getName());
			}
			return (String[]) names.toArray(new String[names.size()]);
		}
		return null;
	}

	public String[] getComponentsForDeployedServiceAssembly(String saName)
			throws Exception {
		Assembly assembly = this.registry.getAssembly(saName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + saName);
		}
		Unit[] units = assembly.getUnits();
		Set names = new HashSet();
		for (int i = 0; i < units.length; i++) {
			names.add(units[i].getComponent().getName());
		}
		return (String[]) names.toArray(new String[names.size()]);
	}

	public boolean isDeployedServiceUnit(String componentName, String suName)
			throws Exception {
		Component comp = this.registry.getComponent(componentName);
		if (comp != null) {
			Unit[] units = comp.getUnits();
			for (int i = 0; i < units.length; i++) {
				if (units[i].getName().equals(suName)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean canDeployToComponent(String componentName) {
		Component comp = this.registry.getComponent(componentName);
		if (comp != null) {
			// TODO: check component state
			ServiceUnitManager mgr = comp.getComponent().getServiceUnitManager();
			return mgr != null;
		}
		return false;
	}

	public String start(String serviceAssemblyName) throws Exception {
		Assembly assembly = this.registry.getAssembly(serviceAssemblyName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + serviceAssemblyName);
		}
		return assembly.start();
	}

	public String stop(String serviceAssemblyName) throws Exception {
		Assembly assembly = this.registry.getAssembly(serviceAssemblyName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + serviceAssemblyName);
		}
		return assembly.stop();
	}

	public String shutDown(String serviceAssemblyName) throws Exception {
		Assembly assembly = this.registry.getAssembly(serviceAssemblyName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + serviceAssemblyName);
		}
		return assembly.shutDown();
	}

	public String getState(String serviceAssemblyName) throws Exception {
		Assembly assembly = this.registry.getAssembly(serviceAssemblyName);
		if (assembly == null) {
			throw new JBIException("Assembly not deployed: " + serviceAssemblyName);
		}
		return assembly.getCurrentState();
	}

}
