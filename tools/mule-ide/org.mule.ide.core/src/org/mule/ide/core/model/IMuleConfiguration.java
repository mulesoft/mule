/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Wraps the details of a single mule configuration file.
 */
public interface IMuleConfiguration {

	/**
	 * Get the unique configuration id.
	 * 
	 * @return the unique id
	 */
	public String getId();

	/**
	 * Get the configuration description.
	 * 
	 * @return the description
	 */
	public String getDescription();

	/**
	 * Get the project-relative path to the configuration file.
	 * 
	 * @return the project-relative path
	 */
	public IPath getFilePath();

	/**
	 * Get the EMF resource for the configuration.
	 * 
	 * @return the EMF resource
	 */
	public Resource getResource();

	/**
	 * Refresh the contents of the configuration from the filesystem.
	 * 
	 * @return a status indicator
	 */
	public IStatus refresh();

	/**
	 * Indicates the status of the configuration. Any status other than IStatus.SUCCESS indicates a
	 * problem loading the configuration.
	 * 
	 * @return the status
	 */
	public IStatus getStatus();
}