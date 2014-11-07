/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;

import org.mule.MessageExchangePattern;
import org.mule.api.client.OperationOptions;
import org.mule.util.Preconditions;

/**
 * Cache key used to locale an {@link org.mule.module.http.internal.request.DefaultHttpRequester} in a map
 * based on a URL and operation options.
 *
 * @param <OptionsType> the expected type for the operation options
 */

public class HttpRequestCacheKey<OptionsType extends OperationOptions>
{

    private final String url;
    private final OptionsType operationOptions;
    private final MessageExchangePattern exchangePattern;

    public HttpRequestCacheKey(final String url, final OptionsType operationOptions, final MessageExchangePattern exchangePattern)
    {
        Preconditions.checkArgument(url != null, "URL cannot be null");
        Preconditions.checkArgument(operationOptions != null, "Operation options cannot be null");
        Preconditions.checkArgument(exchangePattern != null, "Exchange pattern cannot be null");
        this.url = url;
        this.operationOptions = operationOptions;
        this.exchangePattern = exchangePattern;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof HttpRequestCacheKey))
        {
            return false;
        }

        HttpRequestCacheKey that = (HttpRequestCacheKey) o;

        if (!operationOptions.equals(that.operationOptions))
        {
            return false;
        }
        if (!url.equals(that.url))
        {
            return false;
        }
        if (this.exchangePattern != that.exchangePattern)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = url.hashCode();
        result = 31 * result + operationOptions.hashCode();
        result = 31 * result + exchangePattern.hashCode();
        return result;
    }

    public String getUrl()
    {
        return url;
    }

    public OptionsType getOperationOptions()
    {
        return operationOptions;
    }

    public MessageExchangePattern getExchangePattern()
    {
        return exchangePattern;
    }
}
