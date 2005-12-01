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
public interface IMuleConfigSet {

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
}