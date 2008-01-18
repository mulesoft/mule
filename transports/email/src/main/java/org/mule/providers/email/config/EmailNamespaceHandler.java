/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.providers.email.transformers.EmailMessageToString;
import org.mule.providers.email.transformers.MimeMessageToRfc822ByteArray;
import org.mule.providers.email.transformers.ObjectToMimeMessage;
import org.mule.providers.email.transformers.Rfc822ByteArraytoMimeMessage;
import org.mule.providers.email.transformers.StringToEmailMessage;

public class EmailNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("email-to-string-transformer", new TransformerDefinitionParser(EmailMessageToString.class));
        registerBeanDefinitionParser("string-to-email-transformer", new TransformerDefinitionParser(StringToEmailMessage.class));
        registerBeanDefinitionParser("object-to-mime-transformer", new TransformerDefinitionParser(ObjectToMimeMessage.class));
        registerBeanDefinitionParser("mime-to-bytes-transformer", new TransformerDefinitionParser(MimeMessageToRfc822ByteArray.class));
        registerBeanDefinitionParser("bytes-to-mime-transformer", new TransformerDefinitionParser(Rfc822ByteArraytoMimeMessage.class));   
    }

}