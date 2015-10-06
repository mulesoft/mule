/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OAuthProperties;
import org.mule.security.oauth.exception.AuthorizationCodeNotFoundException;
import org.mule.transport.NullPayload;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class ExtractAuthorizationCodeMessageProcessor implements MessageProcessor
{

    private Pattern pattern;

    public ExtractAuthorizationCodeMessageProcessor(Pattern pattern)
    {
        this.pattern = pattern;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            event.getMessage().setInvocationProperty(OAuthProperties.VERIFIER,  extractAuthorizationCode(event));
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Could not extract OAuth verifier"), event, e, this);
        }
        return event;
    }

    private String extractAuthorizationCode(MuleEvent event) throws Exception
    {
        String response;
        if (event.getMessage().getPayload() instanceof NullPayload)
        {
            response = event.getMessage().getInboundProperty("http.query.string");
            if (StringUtils.isBlank(response))
            {
                throw new AuthorizationCodeNotFoundException(this.pattern, response);
            }
        }
        else
        {
            response = event.getMessageAsString();
        }

        Matcher matcher = pattern.matcher(response);
        if (matcher.find() && (matcher.groupCount() >= 1))
        {
            return URLDecoder.decode(matcher.group(1), "UTF-8");
        }
        else
        {
            throw new AuthorizationCodeNotFoundException(this.pattern, response);
        }
    }

}
