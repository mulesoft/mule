/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.voipservice.routers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.samples.voipservice.to.CreditProfileTO;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Binildas Christudas
 */
public class PaymentValidationResponseAggregator extends ResponseCorrelationAggregator {

    protected static transient Log logger = LogFactory.getLog(PaymentValidationResponseAggregator.class);

    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException {

        UMOEvent event = null;
        boolean one = false;
        boolean two = false;
        CreditProfileTO creditProfileTO = null;

        try {
            for (Iterator iterator = events.iterator(); iterator.hasNext();) {
                event = (UMOEvent) iterator.next();
                creditProfileTO = (CreditProfileTO) event.getTransformedMessage();
                if (creditProfileTO.getCreditScore() >= CreditProfileTO.CREDIT_LIMIT) {
                    one = true;
                }
                if (creditProfileTO.getCreditAuthorisedStatus() == CreditProfileTO.CREDIT_AUTHORISED) {
                    two = true;
                }
            }
        } catch (TransformerException e) {
            throw new RoutingException(Message.createStaticMessage("Failed to validate payment service"), new MuleMessage(events, (Map)null), null, e);
        }
        if (one && two && creditProfileTO != null) {
            creditProfileTO.setValid(true);
        }
        return new MuleMessage(creditProfileTO, event.getMessage());
    }

    protected boolean shouldAggregate(EventGroup events) {

        boolean shouldAggregate = super.shouldAggregate(events);
        logger.info("--- *** --- shouldAggregate = " + shouldAggregate + " --- *** ---");
        return shouldAggregate;
    }
}
