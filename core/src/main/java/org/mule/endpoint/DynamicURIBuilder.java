/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import static org.mule.endpoint.URIBuilder.URL_ENCODER;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.expression.ExpressionManager;
import org.mule.config.i18n.CoreMessages;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builds dynamic string URI from a template {@link URIBuilder}
 */
public class DynamicURIBuilder
{

    protected transient final Log logger = LogFactory.getLog(DynamicURIBuilder.class);

    private final URIBuilder templateUriBuilder;

    public DynamicURIBuilder(URIBuilder templateUriBuilder) throws MalformedEndpointException
    {
        validateTemplate(templateUriBuilder.toString());

        this.templateUriBuilder = templateUriBuilder;
    }

    private void validateTemplate(String address) throws MalformedEndpointException
    {
        if (address.indexOf(":") > address.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            throw new MalformedEndpointException(CoreMessages.dynamicEndpointsMustSpecifyAScheme(), address);
        }
    }

    public String build(MuleEvent event) throws URISyntaxException, UnsupportedEncodingException
    {
        String resolvedUri = resolveAddress(event);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Resolved URI from template '%s' to '%s'", templateUriBuilder.getEncodedConstructor(), resolvedUri.toString()));
        }

        return resolvedUri;
    }

    private String resolveAddress(final MuleEvent event) throws URISyntaxException, UnsupportedEncodingException
    {
        final MuleContext muleContext = templateUriBuilder.getMuleContext();
        String resolvedAddress = templateUriBuilder.getTransformedConstructor(new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                String token = (String) input;

                if (muleContext.getExpressionManager().isExpression(token))
                {
                    token = muleContext.getExpressionManager().parse(token, event, true);
                }

                return token;
            }
        }, URL_ENCODER);

        return resolvedAddress;
    }

    public String getUriTemplate()
    {
        return templateUriBuilder.getAddress();
    }

}