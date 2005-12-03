/**
 * Copyright (C) 2003-2005, Cox Communications, Inc.
 */
package org.mule.ide.internal.core.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.mule.ide.core.model.IMuleModelElement;

/**
 * Abstract base class for elements in the Mule model.
 */
public abstract class MuleModelElement implements IMuleModelElement {

	/** Indicates whether the config was resolved and loaded */
	private IStatus status = Status.OK_STATUS;

	/**
	 * Sets the 'status' field.
	 * 
	 * @param status The 'status' value.
	 */
	protected void setStatus(IStatus status) {
		this.status = status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#getStatus()
	 */
	public IStatus getStatus() {
		return status;
	}
}