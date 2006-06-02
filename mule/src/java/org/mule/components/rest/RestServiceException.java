/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.components.rest;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RestServiceException extends MessagingException
{
    public RestServiceException(Message message, UMOMessage umoMessage) {
        super(message, umoMessage);
    }

    public RestServiceException(Message message, UMOMessage umoMessage, Throwable cause) {
        super(message, umoMessage, cause);
    }
}
