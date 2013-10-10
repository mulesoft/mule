/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.example.bookstore.Book;
import org.mule.example.bookstore.Order;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.email.MailProperties;

/**
 * Composes an e-mail notification message to be sent based on the Book Order.
 */
public class OrderToEmailTransformer extends AbstractMessageTransformer
{
    public OrderToEmailTransformer()
    {
        super();
        registerSourceType(DataTypeFactory.create(Order.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Order order = (Order) message.getPayload();
        Book book = order.getBook();

        String body =  "Thank you for placing your order for " +
                       book.getTitle() + " with the Mule-powered On-line Bookstore. " +
                       "Your order will be shipped  to " +
                       order.getAddress() + " by the next business day.";

        String email = order.getEmail();
        message.setOutboundProperty(MailProperties.TO_ADDRESSES_PROPERTY, email);

        logger.info("Sending e-mail notification to " + email);
        return body;
    }
}
