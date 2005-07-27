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
package org.mule.jbi.messaging;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * Is thrown when an Out message is expected on the Message exchange, but none
 * is available
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NoMessageException extends MessagingException
{
    public NoMessageException(MessageExchange me, String name) {
        super("There is no '" + name + "' message on message exchange: " + me);
    }
    
    public NoMessageException(MessageExchange me, String name, Exception e) {
        super("There is no  '" + name + "'  message on message exchange: " + me, e);
    }
}
