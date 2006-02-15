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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.resource.Resource;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.jobs.UpdateMarkersForEcoreResourceJob;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.core.nature.MuleNature;

/**
 * Plugin for all Mule IDE core functionality.
 */
public class MuleCorePlugin extends Plugin {

	/** Plugin instance */
	static private MuleCorePlugin defaultPlugin = null;

	/** Eclipse plugin id */
	public static final String PLUGIN_ID = "org.mule.ide.core";

	/** Unique for the Mule classpath container */
	public static final String ID_MULE_CLASSPATH_CONTAINER = PLUGIN_ID + ".MULE_CONTAINER";

	/** Eclipse variable that holds the Mule external root folder location */
	public static final String ID_MULE_EXTERNAL_ROOT = "MULEROOT";

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
	 * Sets or clears the Mule UMO Configuration nature to this project
	 * 
	 * @param project The project to set.
	 * @param setIt True if the nature should be added, false if it should be removed
	 * @throws CoreException If something goes wrong
	 */
	public void setMuleNature(IProject project, boolean setIt) throws CoreException {
		/*
		 * Four possible outcomes: A - transition to on B - already on C - transition to off D - already off
		 */
		if (project == null)
			return;

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (MuleNature.NATURE_ID.equals(natures[i])) {
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
		newNatures[0] = MuleNature.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	/**
	 * Get the Mule nature associated with the project.
	 * 
	 * @param project the project
	 * @return the nature or null if nature not configured
	 */
	public MuleNature getMuleNature(IProject project) {
		try {
			return (MuleNature) project.getNature(MuleNature.NATURE_ID);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Indicates whether the given project has a Mule nature assigned.
	 * 
	 * @param project the project to check
	 * @return true if has nature, false if not
	 */
	public boolean hasMuleNature(IProject project) {
		return (getMuleNature(project) != null);
	}

	/**
	 * Get the Mule model for the given project.
	 * 
	 * @param project the project
	 * @return the model, or null if nature not configured
	 * @throws MuleModelException
	 */
	public IMuleModel getMuleModel(IProject project) throws MuleModelException {
		MuleNature nature = getMuleNature(project);
		if (nature != null) {
			IMuleModel model = nature.getMuleModel();
			if (model != null) {
				return model;
			}
			throw new MuleModelException(createErrorStatus("Mule model is null for project: "
					+ project.getName(), null));
		}
		throw new MuleModelException(createErrorStatus("Project does not have the Mule nature configured: "
				+ project.getName(), null));
	}

	/**
	 * Get the list of project that have a Mule nature assigned.
	 * 
	 * @return the list of IProject objects
	 */
	public IProject[] getMuleProjects() {
		List result = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (hasMuleNature(projects[i])) {
				result.add(projects[i]);
			}
		}
		return (IProject[]) result.toArray(new IProject[result.size()]);
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
		createMarker(resource, severity, message, null);
	}

	/**
	 * Create a marker on the given resource
	 * 
	 * @param resource the resource
	 * @param severity the severity constant
	 * @param message the message
	 * @param lineNumber the line number
	 */
	public void createMarker(IResource resource, int severity, String message, Integer lineNumber) {
		try {
			IMarker marker = resource.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
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
			resource.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			logException(e.getMessage(), e);
		}
	}

	/**
	 * Gets the errors and warnings for the Ecore resource and adds them as markers for the given Eclipse
	 * resource. Runs as a background job so that workspace locks do not prevent adding markers.
	 * 
	 * @param eclipseResource the Eclipse resource
	 * @param ecoreResource the Ecore resource wrapper
	 */
	public void updateMarkersForEcoreResource(IResource eclipseResource, Resource ecoreResource) {
		UpdateMarkersForEcoreResourceJob job = new UpdateMarkersForEcoreResourceJob(eclipseResource,
				ecoreResource);
		job.setPriority(Job.SHORT);
		job.schedule();
	}
}