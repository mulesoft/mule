package org.mule.ide.core.exception;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Exception thrown when there is a problem with the Mule model.
 */
public class MuleModelException extends CoreException {

	public MuleModelException(IStatus status) {
		super(status);
	}
}