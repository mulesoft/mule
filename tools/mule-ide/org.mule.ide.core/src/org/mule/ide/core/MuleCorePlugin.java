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
package org.mule.ide.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.core.preferences.PreferenceConstants;

/**
 * @author Jesper
 * 
 */
public class MuleCorePlugin extends Plugin {

	static private MuleCorePlugin defaultPlugin = null;

	/** Eclipse plugin id */
	public static final String PLUGIN_ID = "org.mule.ide.core";

	/** Unique for the Mule classpath container */
	public static final String ID_MULE_CLASSPATH_CONTAINER = PLUGIN_ID + ".MuleLibraries";

	/** Problem marker id */
	public static final String MARKER_TYPE = "org.mule.ide.core.xmlProblem";

	public MuleCorePlugin() {
		super();
		defaultPlugin = this;
	}

	/**
	 * @return The singleton instance of the MuleCorePlugin
	 */
	static public MuleCorePlugin getDefault() {
		return defaultPlugin;
	}

	/**
	 * 
	 * @return The path of base of Mule executables, or null
	 */
	public File getMulePath() {
		String path = getPluginPreferences().getString(PreferenceConstants.P_MULEPATH);
		if (path.length() < 2)
			return null;
		return new File(path);
	}

	/**
	 * Sets or clears the Mule UMO Configuration nature to this project
	 * 
	 * @param project The project to set.
	 * @param setIt True if the nature should be added, false if it should be removed
	 * @throws CoreException If something goes wrong
	 */
	public void setMuleNature(IProject project, boolean setIt) throws CoreException {
		/*
		 * Four possible outcomes: A - transition to on B - already on C - transition to off D -
		 * already off
		 */
		if (project == null)
			return;

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (MuleConfigNature.NATURE_ID.equals(natures[i])) {
				if (setIt)
					return; // outcome B - Already had the nature

				// Outcome C - Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				return; // Outcome C - No longer has the nature
			}
		}
		if (!setIt)
			return; // Outcome D - didn't have it, just do nothing

		// Outcome A - add the nature
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 1, natures.length);
		newNatures[0] = MuleConfigNature.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	/**
	 * Get the Mule nature associated with the project.
	 * 
	 * @param project the project
	 * @return the nature or null if nature not configured
	 */
	public MuleConfigNature getMuleNature(IProject project) {
		try {
			return (MuleConfigNature) project.getNature(MuleConfigNature.NATURE_ID);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Get the Mule model for the given project.
	 * 
	 * @param project the project
	 * @return the model, or null if nature not configured
	 */
	public IMuleModel getMuleModel(IProject project) {
		MuleConfigNature nature = getMuleNature(project);
		if (nature != null) {
			return nature.getMuleModel();
		}
		return null;
	}

	/**
	 * Create a container entry for the Mule classpath manager.
	 * 
	 * @return the classpath entry
	 */
	public static IClasspathEntry createMuleClasspathContainerEntry() {
		return JavaCore.newContainerEntry(new Path(MuleCorePlugin.ID_MULE_CLASSPATH_CONTAINER));
	}

	/**
	 * Create classpath entries for all jars in the "lib" folder of the plugin.
	 * 
	 * @return the array of entries
	 */
	public IClasspathEntry[] getMuleLibraries() {
		Enumeration urls = getBundle().findEntries("lib", "*.jar", false);
		List classpath = new ArrayList();
		while (urls.hasMoreElements()) {
			URL url = (URL) urls.nextElement();
			try {
				url = Platform.resolve(url);
				IPath filePath = new Path(new File(url.getFile()).getAbsolutePath());
				IClasspathEntry entry = JavaCore.newLibraryEntry(filePath, null, null);
				classpath.add(entry);
			} catch (IOException e) {
				logException("Unable to add library to classpath.", e);
			}
		}
		return (IClasspathEntry[]) classpath.toArray(new IClasspathEntry[classpath.size()]);
	}

	/**
	 * Create a MultiStatus with the given message.
	 * 
	 * @param message the message
	 * @return the status
	 */
	public MultiStatus createMultiStatus(String message) {
		return new MultiStatus(MuleCorePlugin.PLUGIN_ID, 0, message, null);
	}

	/**
	 * Create a status of a given type.
	 * 
	 * @param type the IStatus constant
	 * @param message the message
	 * @param exception the exception (may be null)
	 * @return the status
	 */
	public IStatus createStatus(int type, String message, Throwable exception) {
		return new Status(type, PLUGIN_ID, 0, message, exception);
	}

	/**
	 * Create an error status.
	 * 
	 * @param message the error message
	 * @param exception the exception (may be null)
	 * @return the status
	 */
	public IStatus createErrorStatus(String message, Throwable exception) {
		return createStatus(IStatus.ERROR, message, exception);
	}

	/**
	 * Log an exception to the Eclipse error log.
	 * 
	 * @param message the error message
	 * @param exception the exception
	 */
	public void logException(String message, Throwable exception) {
		IStatus status = createErrorStatus(message, exception);
		this.getLog().log(status);
	}

	/**
	 * Log an info message to the Eclipse log.
	 * 
	 * @param message the message
	 */
	public void logInfo(String message) {
		IStatus status = createStatus(IStatus.INFO, message, null);
		this.getLog().log(status);
	}

	/**
	 * Create a marker on the given resource.
	 * 
	 * @param resource the resource
	 * @param severity the severity constant
	 * @param message the error message
	 */
	public void createMarker(IResource resource, int severity, String message) {
		try {
			IMarker marker = resource.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
		} catch (CoreException e) {
			logException(e.getMessage(), e);
		}
	}

	/**
	 * Clear the markers associated with a resource.
	 * 
	 * @param resource the resource
	 */
	public void clearMarkers(IResource resource) {
		try {
			resource.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			logException(e.getMessage(), e);
		}
	}
}