/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jbi.components;

import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;

/**
 * Mule transformers can be reused in side a Jbi container
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerComponent extends AbstractJbiComponent {

    protected UMOTransformer transformer = null;

    public void onMessage(MessageExchange messageExchange, NormalizedMessage message) throws MessagingException {
        if (messageExchange.getRole() == MessageExchange.Role.PROVIDER) {
            return;
        }
        NormalizedMessage in = messageExchange.getMessage("in");


        try {
            NormalizedMessage out = messageExchange.createMessage();
            transform(messageExchange, in, out);

            if(messageExchange instanceof InOut) {
                messageExchange.setMessage(out, OUT);
            }
            else {
                InOnly outExchange = exchangeFactory.createInOnlyExchange();
                outExchange.setInMessage(out);
                deliveryChannel.sendSync(outExchange);
            }
            done(messageExchange);
        }
        catch (Exception e) {
            error(messageExchange, e);
        }
    }

    protected void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws TransformerException, MessagingException {
        Object result = transformer.transform(in.getContent());
        if(result instanceof Source) {
            out.setContent((Source)result);
        } else {
            throw new UnsupportedOperationException("Support for Source transformation is not yet implemented");
        }
    }
}
