/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.config;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.util.StringUtils.isEmpty;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.transport.jms.JmsTransactionFactory;
import org.mule.util.OneTimeWarning;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


public class JmsTransactionDefinitionParser extends TransactionDefinitionParser
{

    private static final String TIMEOUT_WARNING_MESSAGE = format("The timeout attribute on the <jms:transaction> element will be ignored since it is only taken into account in XA Transactions.");
    private static final OneTimeWarning timeoutAttributeWarning = new OneTimeWarning(getLogger(JmsTransactionDefinitionParser.class), TIMEOUT_WARNING_MESSAGE);

    public JmsTransactionDefinitionParser()
    {
        super(JmsTransactionFactory.class);
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        if(!isEmpty(element.getAttribute(TIMEOUT)))
        {
            timeoutAttributeWarning.warn();
        }

        return super.parseInternal(element, parserContext);
    }

}
