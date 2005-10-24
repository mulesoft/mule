/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Jesper Steen Møller. All rights reserved.
 * http://www.selskabet.org/jesper/
 * 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.ide.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.mule.ide.core.MuleCorePlugin;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MuleConfigLaunchConfigurationDelegate extends JavaLaunchDelegate {
		
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, mode, launch, monitor);
	}
	
	static void collectFiles(File under, FileFilter filter, Collection into) {
		File[] files = under.listFiles(filter);
		for (int i=0; i<files.length; ++i) {
			if (! files[i].isDirectory()) into.add(files[i].getAbsolutePath());
		}
		for (int i=0; i<files.length; ++i) {
			if (files[i].isDirectory()) collectFiles(files[i], filter, into);
		}
	}
	
	/**
	 * Returns the entries that should appear on the user portion of the
	 * classpath as specified by the given launch configuration, as an array of
	 * resolved strings. The returned array is empty if no classpath is
	 * specified.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the classpath specified by the given launch configuration,
	 *         possibly an empty array
	 * @throws CoreException 
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		
		String[] superClasspath = super.getClasspath(configuration);
		File muleRoot = MuleCorePlugin.getDefault().getMulePath();
		if (muleRoot == null)
			return superClasspath;

		List l = new ArrayList();
		for (int i = 0; i < superClasspath.length; ++i)
			l.add(superClasspath[i]);
		
		collectFiles(muleRoot, new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
			}
		}, l);
		
		return (String[])l.toArray(new String[l.size()]);
		
		/*
		 *  This is how you might pull in the classpath of a specific plug-in, must it should be adjusted for the new .jar plug-in packaging
		URL runtimeURL= Platform.getBundle("org.mule.ide.runtime").getEntry("/"); //$NON-NLS-1$ //$NON-NLS-2$
		URL modelURL= Platform.getBundle("org.mule.ide.model").getEntry("/"); //$NON-NLS-1$
		
		String[] cp= super.getClasspath(configuration);
		String[] classPath= null;
		
		try {
			if (Platform.inDevelopmentMode()) {
				// we first try the bin output folder
				List junitEntries= new ArrayList();
				
				try {
					junitEntries.add(Platform.asLocalURL(new URL(modelURL, "bin")).getFile()); //$NON-NLS-1$
				} catch (IOException e3) {
					try {
						junitEntries.add(Platform.asLocalURL(new URL(modelURL, "muleidemodel.jar")).getFile()); //$NON-NLS-1$
					} catch (IOException e4) {
						// fall through
					}
				}
				try {
					junitEntries.add(Platform.asLocalURL(new URL(runtimeURL, "bin")).getFile()); //$NON-NLS-1$
				} catch (IOException e1) {
					try {
						junitEntries.add(Platform.asLocalURL(new URL(runtimeURL, "muleumoruntime.jar")).getFile()); //$NON-NLS-1$
					} catch (IOException e4) {
						// fall through
					}
				}
				classPath= new String[cp.length + junitEntries.size()];
				Object[] jea= junitEntries.toArray();
				System.arraycopy(cp, 0, classPath, 0, cp.length);
				System.arraycopy(jea, 0, classPath, cp.length, jea.length);
			} else {
				classPath= new String[cp.length + 2];
				System.arraycopy(cp, 0, classPath, 0, cp.length);
				classPath[cp.length]= Platform.asLocalURL(new URL(modelURL, "muleidemodel.jar")).getFile(); //$NON-NLS-1$
				classPath[cp.length + 1]= Platform.asLocalURL(new URL(runtimeURL, "mflowruntime.jar")).getFile(); //$NON-NLS-1$
			}
		} catch (IOException e) {
			//JUnitPlugin.log(e); // TODO abort run and inform user
		}*/
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {

		String configName = configuration.getAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, (String)null);
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
		if (configName == null || projectName == null) 
			abort("Missing launcher data for Mule UMO config launch", null, 42);

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project == null) 
			abort("No project found called " + projectName, null, 43);
		
		IFile file = project.getFile(configName);
		if (file == null) 
			abort("No file found called " + configName, null, 43);
		
		return "-config \"" + file.getRawLocation() + "\"";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		StringBuffer arguments = new StringBuffer(super.getVMArguments(configuration));
		return arguments.toString();
	}
 	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_FLOWRUNNER_CLASS, IMuleConfigLaunchConfigurationConstants.DEFAULT_FLOWRUNNER_CLASS);
	}
	
}
