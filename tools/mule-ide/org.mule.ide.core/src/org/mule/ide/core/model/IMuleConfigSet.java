/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import java.util.List;

import org.eclipse.core.runtime.IPath;

/**
 * An ordered list of related IMuleConfiguration objects.
 */
public interface IMuleConfigSet extends IMuleModelElement {

	/**
	 * Get the unique id.
	 * 
	 * @return the unique id
	 */
	public String getId();

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription();

	/**
	 * Set the description.
	 * 
	 * @param description the description
	 */
	public void setDescription(String description);

	/**
	 * Get the ordered list of Mule configurations used in this config set.
	 * 
	 * @return the ordered list of configurations
	 */
	public List getMuleConfigurations();

	/**
	 * Get the path handles for all config files in the set.
	 * 
	 * @return the array of paths
	 */
	public IPath[] getConfigFilePaths();

	/**
	 * Add the given configuration to the set.
	 * 
	 * @param config the config to add
	 */
	public void addConfiguration(IMuleConfiguration config);

	/**
	 * Remove the given configuration from the set.
	 * 
	 * @param config the config to remove
	 */
	public void removeConfiguration(IMuleConfiguration config);

	/**
	 * Indicates if the given configuration is the first for this config set.
	 * 
	 * @param config the configuration
	 * @return true if first, false if not
	 */
	public boolean isFirstConfiguration(IMuleConfiguration config);

	/**
	 * Indicates if the given configuration is the last for this config set.
	 * 
	 * @param config the configuration
	 * @return true if last, false if not
	 */
	public boolean isLastConfiguration(IMuleConfiguration config);

	/**
	 * Increase the priority of a configuration within the set.
	 * 
	 * @param config the configuration
	 */
	public void increasePriority(IMuleConfiguration config);

	/**
	 * Decrease the priority of a configuration within the set.
	 * 
	 * @param config the configuration
	 */
	public void decreasePriority(IMuleConfiguration config);
}