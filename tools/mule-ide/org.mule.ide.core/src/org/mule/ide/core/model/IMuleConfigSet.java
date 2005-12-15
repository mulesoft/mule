/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import java.util.List;

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
	 * Get the ordered list of Mule configurations used in this config set.
	 * 
	 * @return the ordered list of configurations
	 */
	public List getMuleConfigurations();

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
}