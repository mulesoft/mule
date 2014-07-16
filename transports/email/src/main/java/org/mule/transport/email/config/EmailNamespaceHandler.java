/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.transport.email.transformers.EmailMessageToString;
import org.mule.transport.email.transformers.MimeMessageToRfc822ByteArray;
import org.mule.transport.email.transformers.ObjectToMimeMessage;
import org.mule.transport.email.transformers.Rfc822ByteArraytoMimeMessage;
import org.mule.transport.email.transformers.StringToEmailMessage;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Flags;

public class EmailNamespaceHandler extends AbstractMuleNamespaceHandler
{
    protected static ValueMap DEFAULT_PROCESS_MESSAGE_ACTION;

    static
    {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("ANSWERED", Flags.Flag.ANSWERED);
        mapping.put("DELETED", Flags.Flag.DELETED);
        mapping.put("DRAFT", Flags.Flag.DRAFT);
        mapping.put("FLAGGED", Flags.Flag.FLAGGED);
        mapping.put("RECENT", Flags.Flag.RECENT);
        mapping.put("SEEN", Flags.Flag.SEEN);
        mapping.put("USER", Flags.Flag.USER);
        mapping.put("NONE", null);
        DEFAULT_PROCESS_MESSAGE_ACTION = new SimplePropertyConfiguration.IndentityMapValueMap(mapping);
    }

    @Override
    public void init()
    {
        registerBeanDefinitionParser("email-to-string-transformer", new MessageProcessorDefinitionParser(EmailMessageToString.class));
        registerBeanDefinitionParser("string-to-email-transformer", new MessageProcessorDefinitionParser(StringToEmailMessage.class));
        registerBeanDefinitionParser("object-to-mime-transformer", new MessageProcessorDefinitionParser(ObjectToMimeMessage.class));
        registerBeanDefinitionParser("mime-to-bytes-transformer", new MessageProcessorDefinitionParser(MimeMessageToRfc822ByteArray.class));
        registerBeanDefinitionParser("bytes-to-mime-transformer", new MessageProcessorDefinitionParser(Rfc822ByteArraytoMimeMessage.class));
    }
}
