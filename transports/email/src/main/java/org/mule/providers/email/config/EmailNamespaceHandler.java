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
        registerBeanDefinitionParser("transformer-email-to-string", new TransformerDefinitionParser(EmailMessageToString.class));   
        registerBeanDefinitionParser("transformer-string-to-email", new TransformerDefinitionParser(StringToEmailMessage.class));   
        registerBeanDefinitionParser("transformer-object-to-mime", new TransformerDefinitionParser(ObjectToMimeMessage.class));   
        registerBeanDefinitionParser("transformer-mime-to-bytes", new TransformerDefinitionParser(MimeMessageToRfc822ByteArray.class));   
        registerBeanDefinitionParser("transformer-bytes-to-mime", new TransformerDefinitionParser(Rfc822ByteArraytoMimeMessage.class));   
    }

}