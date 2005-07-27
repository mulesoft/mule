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
package org.mule.jbi.components.simple;

import org.mule.jbi.components.mule.AbstractMuleComponent;
import org.mule.jbi.messaging.MessageListener;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EchoComponent extends AbstractMuleComponent implements MessageListener
{
    public void onMessage(MessageExchange me) throws MessagingException {
        NormalizedMessage message = getInMessage(me);
        me.setMessage(message, OUT);
    }
}
