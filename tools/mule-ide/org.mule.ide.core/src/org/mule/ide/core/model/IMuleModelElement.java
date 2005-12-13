/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.core.model;

import org.eclipse.core.runtime.IStatus;

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

	/**
	 * Refresh the given element and return a status indicating result.
	 * 
	 * @return a status indicator
	 */
	public IStatus refresh();

	/**
	 * Get the status of the element.
	 * 
	 * @return the status
	 */
	public IStatus getStatus();

	/**
	 * Get the label shown in views for the element.
	 * 
	 * @return the label
	 */
	public String getLabel();
}