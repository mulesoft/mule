/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Wraps the details of a single mule configuration file.
 */
public interface IMuleConfiguration extends IMuleModelElement, Comparable {

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
	 * Get the label that is shown in various views.
	 * 
	 * @return the label
	 */
	public String getLabel();

	/**
	 * Gets the project-relative path to the config file.
	 * 
	 * @return the path
	 */
	public String getRelativePath();

	/**
	 * Get the project-relative IPath to the configuration file.
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
}