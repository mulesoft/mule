
 
package org.mule.samples.errorhandler;

import org.mule.umo.UMOException;

/**
 *  <code>HandlerException</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class HandlerException extends UMOException {

	public HandlerException(String message) {
		super(message);
	}
	
	public HandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
