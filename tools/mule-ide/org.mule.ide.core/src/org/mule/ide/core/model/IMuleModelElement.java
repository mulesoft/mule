/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

/**
 * Common base interface for elements in the Mule IDE model.
 */
public interface IMuleModelElement {

	/**
	 * Get the model this element belongs to.
	 * 
	 * @return the model
	 */
	public IMuleModel getMuleModel();
}