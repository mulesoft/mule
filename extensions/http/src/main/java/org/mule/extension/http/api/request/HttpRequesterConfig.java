/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.net.CookieManager;
import java.util.function.Function;

import javax.inject.Inject;

/**
 * Configuration element for a HTTP requests.
 *
 * @since 4.0
 */
@Configuration(name = "request-config")
@Providers(HttpRequesterProvider.class)
@Operations({HttpRequesterOperations.class})
public class HttpRequesterConfig implements Initialisable, Stoppable
{
    @ConfigName
    private String configName;

    //TODO: document
    @Parameter
    @Optional
    private HttpAuthentication authentication;

    /**
     * Base path to use for all requests that reference this config.
     */
    @Parameter
    @Optional(defaultValue = "/")
    private Function<MuleEvent, String> basePath;

    /**
     * Specifies whether to follow redirects or not. Default value is true.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private Function<MuleEvent, Boolean> followRedirects;

    /**
     * By default, the response will be parsed (for example, a multipart response will be mapped as a
     * Mule message with null payload and inbound attachments with each part). If this property is set to false,
     * no parsing will be done, and the payload will always contain the raw contents of the HTTP response.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private Function<MuleEvent, Boolean> parseResponse;

    /**
     * Defines if the request should be sent using streaming or not. If this attribute is not present,
     * the behavior will depend on the type of the payload (it will stream only for InputStream). If set
     * to true, it will always stream. If set to false, it will never stream. As streaming is done the request
     * will be sent user Transfer-Encoding: chunked.
     */
    @Parameter
    @Optional(defaultValue = "AUTO")
    private Function<MuleEvent, HttpStreamingType> requestStreamingMode;

    /**
     * Defines if the request should contain a body or not. If AUTO, it will depend on the method (GET,
     * HEAD and OPTIONS will not send a body).
     */
    @Parameter
    @Optional(defaultValue = "AUTO")
    private Function<MuleEvent, HttpSendBodyMode> sendBodyMode;

    /**
     * Maximum time that the request element will block the execution of the flow waiting for the HTTP response.
     * If this value is not present, the default response timeout from the Mule configuration will be used.
     */
    @Parameter
    @Optional
    private Function<MuleEvent, Integer> responseTimeout;

    /**
     * If true, cookies received in HTTP responses will be stored, and sent in subsequent HTTP requests.
     */
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private boolean enableCookies;

    /**
     * Specifies a RAML configuration file for the API that is being consumed.
     */
    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private RamlApiConfiguration apiConfiguration;

    @Inject
    private MuleContext muleContext;
    private CookieManager cookieManager;
    private boolean stopped = false;

    public String getName()
    {
        return configName;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (enableCookies)
        {
            cookieManager = new CookieManager();
        }
    }

    public Function<MuleEvent, String> getBasePath()
    {
        return basePath;
    }

    public Function<MuleEvent, Boolean> getFollowRedirects()
    {
        return followRedirects;
    }

    public Function<MuleEvent, Boolean> getParseResponse()
    {
        return parseResponse;
    }

    public Function<MuleEvent, HttpStreamingType> getRequestStreamingMode()
    {
        return requestStreamingMode;
    }

    public Function<MuleEvent, HttpSendBodyMode> getSendBodyMode()
    {
        return sendBodyMode;
    }

    public Function<MuleEvent, Integer> getResponseTimeout()
    {
        return responseTimeout;
    }

    public HttpAuthentication getAuthentication()
    {
        return authentication;
    }

    public boolean isEnableCookies()
    {
        return enableCookies;
    }

    public RamlApiConfiguration getApiConfiguration()
    {
        return apiConfiguration;
    }

    public CookieManager getCookieManager()
    {
        return cookieManager;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(this.authentication);
        stopped = true;
    }

    public boolean isStopped()
    {
        return stopped;
    }
}
