
 
package org.mule.samples.errorhandler;

import org.mule.umo.UMOException;
import org.mule.config.i18n.Message;

/**
 *  <code>HandlerException</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class HandlerException extends UMOException {

	public HandlerException(String message) {
		super(Message.createStaticMessage(message));
	}
	
	public HandlerException(String message, Throwable cause) {
		super(Message.createStaticMessage(message), cause);
	}
}
