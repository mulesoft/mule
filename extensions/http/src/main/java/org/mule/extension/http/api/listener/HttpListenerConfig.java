/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.listener.ListenerPath;

/**
 * Configuration element for a {@link HttpListener}.
 *
 * @since 4.0
 */
@Configuration(name = "listener-config")
@Providers(HttpListenerProvider.class)
@Sources(HttpListener.class)
public class HttpListenerConfig implements Initialisable
{
    @ConfigName
    private String configName;

    /**
     * Base path to use for all requests that reference this config.
     */
    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private String basePath;

    /**
     * By default, the request will be parsed (for example, a multi part request will be mapped as a
     * Mule message with no payload and attributes with each part). If this property is set to false,
     * no parsing will be done, and the payload will always contain the raw contents of the HTTP request.
     */
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private Boolean parseRequest;


    @Override
    public void initialise() throws InitialisationException
    {
        basePath = HttpParser.sanitizePathWithStartSlash(this.basePath);
    }

    public ListenerPath getFullListenerPath(String listenerPath)
    {
        checkArgument(listenerPath.startsWith("/"), "listenerPath must start with /");
        return new ListenerPath(basePath, listenerPath);
    }

    public Boolean resolveParseRequest(Boolean listenerParseRequest)
    {
        return listenerParseRequest != null ? listenerParseRequest : (parseRequest != null ? parseRequest : true);
    }

    public String getConfigName()
    {
        return configName;
    }
}
