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
package org.mule.jbi.components.mule;

import org.mule.jbi.components.AbstractComponent;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerComponent extends AbstractComponent {

    protected UMOTransformer transformer = null;

    public void onMessage(MessageExchange messageExchange, NormalizedMessage message) throws MessagingException {
        if (messageExchange.getRole() == MessageExchange.Role.PROVIDER) {
			//Todo throw exception here?
            return;
		}
        NormalizedMessage in = messageExchange.getMessage("in");


        try {
            NormalizedMessage out = messageExchange.createMessage();
            transform(messageExchange, in, out);

            if (isInOut(messageExchange)) {
                messageExchange.setMessage(out, OUT);
            }
            else {
                InOnly outExchange = getExchangeFactory().createInOnlyExchange();
                outExchange.setInMessage(out);
                context.getDeliveryChannel().sendSync(outExchange);
            }
            done(messageExchange);
        }
        catch (Exception e) {
            error(messageExchange, e);
        }
    }

    protected void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws TransformerException {
        Object result = transformer.transform(in.getContent());
//        out.setContent();
//        RequestContext.
    }
}
