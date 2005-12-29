/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.mule.ide.core.exception.MuleModelException;

/**
 * Represents the model for a single Mule project. The model consists of zero or more Mule
 * configurations and zero or more Mule config sets.
 */
public interface IMuleModel extends IMuleModelElement {

	/**
	 * Gets the project that contains this model.
	 * 
	 * @return the project
	 */
	public IProject getProject();

	/**
	 * Creates a duplicate copy of the model that may be modified without changing the real model.
	 * 
	 * @return a copy of the model
	 * @throws MuleModelException if problem duplicating the model
	 */
	public IMuleModel createWorkingCopy() throws MuleModelException;

	/**
	 * Persist the model.
	 * 
	 * @throws MuleModelException if the model could not be saved
	 */
	public void save() throws MuleModelException;

	/**
	 * Empties the list of Mule configuration files.
	 */
	public void clearMuleConfigurations();

	/**
	 * Creates a new Mule configuration.
	 * 
	 * @param description description of configuration
	 * @param relativePath project-relative path
	 * @return the created configuration
	 */
	public IMuleConfiguration createNewMuleConfiguration(String description, String relativePath);

	/**
	 * Empties the list of Mule configuration sets.
	 */
	public void clearMuleConfigSets();

	/**
	 * Creates a new Mule config set.
	 * 
	 * @param description description of the config set
	 * @return the created config set
	 */
	public IMuleConfigSet createNewMuleConfigSet(String description);

	/**
	 * Get the configurations associated with the model.
	 * 
	 * @return the configurations
	 */
	public Collection getMuleConfigurations();

	/**
	 * Get the Mule configuration element that corresponds to the given path.
	 * 
	 * @param path the path
	 * @return the config element or null if not found
	 */
	public IMuleConfiguration getMuleConfigurationForPath(IPath path);

	/**
	 * Get the config sets associated with the model.
	 * 
	 * @return
	 */
	public Collection getMuleConfigSets();

	/**
	 * Add a configuration to the model.
	 * 
	 * @param muleConfiguration the configuration to add
	 */
	public void addMuleConfiguration(IMuleConfiguration muleConfiguration);

	/**
	 * Add a config set to the model.
	 * 
	 * @param muleConfigSet
	 */
	public void addMuleConfigSet(IMuleConfigSet muleConfigSet);

	/**
	 * Get a configuration based on its unique id.
	 * 
	 * @param id the unique id
	 * @return the configuration or null if not found
	 */
	public IMuleConfiguration getMuleConfiguration(String id);

	/**
	 * Ge the config set based on its unique id.
	 * 
	 * @param id the unique id
	 * @return the config set or null if not found
	 */
	public IMuleConfigSet getMuleConfigSet(String id);

	/**
	 * Remove the given mule configuration based on its unique id.
	 * 
	 * @param id the id of the configuration to remove
	 * @return the configuration that was removed or null if not found
	 */
	public IMuleConfiguration removeMuleConfiguration(String id);

	/**
	 * Remove the given mule config set based on its unique id.
	 * 
	 * @param id the id of the config set to remove
	 * @return the config set that was removed or null if not found
	 */
	public IMuleConfigSet removeMuleConfigSet(String id);
}