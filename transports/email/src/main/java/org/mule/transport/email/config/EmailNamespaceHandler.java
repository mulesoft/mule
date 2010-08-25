/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.transport.email.transformers.EmailMessageToString;
import org.mule.transport.email.transformers.MimeMessageToRfc822ByteArray;
import org.mule.transport.email.transformers.ObjectToMimeMessage;
import org.mule.transport.email.transformers.Rfc822ByteArraytoMimeMessage;
import org.mule.transport.email.transformers.StringToEmailMessage;

public class EmailNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("email-to-string-transformer", new MessageProcessorDefinitionParser(EmailMessageToString.class));
        registerBeanDefinitionParser("string-to-email-transformer", new MessageProcessorDefinitionParser(StringToEmailMessage.class));
        registerBeanDefinitionParser("object-to-mime-transformer", new MessageProcessorDefinitionParser(ObjectToMimeMessage.class));
        registerBeanDefinitionParser("mime-to-bytes-transformer", new MessageProcessorDefinitionParser(MimeMessageToRfc822ByteArray.class));
        registerBeanDefinitionParser("bytes-to-mime-transformer", new MessageProcessorDefinitionParser(Rfc822ByteArraytoMimeMessage.class));   
    }

}
