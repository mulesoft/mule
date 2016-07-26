/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import org.mule.extension.http.api.request.authentication.BasicAuthentication;
import org.mule.extension.http.api.request.authentication.DigestAuthentication;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.authentication.NtlmAuthentication;
import org.mule.extension.http.api.request.proxy.DefaultProxyConfig;
import org.mule.extension.http.api.request.proxy.NtlmProxyConfig;
import org.mule.extension.http.api.request.proxy.ProxyConfig;
import org.mule.extension.http.api.request.validator.FailureStatusCodeValidator;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.api.request.validator.SuccessStatusCodeValidator;
import org.mule.extension.http.api.server.HttpListenerConnectionManager;
import org.mule.extension.http.internal.listener.server.HttpListenerConfig;
import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * HTTP connector used to handle and perform HTTP requests.
 * <p>
 * This class only serves as an extension definition. It's configurations are divided on server
 * ({@link HttpListenerConfig}) and client ({@link HttpRequesterConfig}) capabilities.
 *
 * @since 4.0
 */
@Extension(name = "HTTP", description = "Connector to handle and perform HTTP requests")
@Configurations({HttpListenerConfig.class, HttpRequesterConfig.class})
@Operations(HttpOperations.class)
@SubTypeMapping(baseType = HttpAuthentication.class, subTypes = {BasicAuthentication.class, DigestAuthentication.class, NtlmAuthentication.class})
@SubTypeMapping(baseType = ProxyConfig.class, subTypes = {DefaultProxyConfig.class, NtlmProxyConfig.class})
@SubTypeMapping(baseType = ResponseValidator.class, subTypes = {SuccessStatusCodeValidator.class, FailureStatusCodeValidator.class})
@Import(type = TcpClientSocketProperties.class, from = SocketsExtension.class)
@Import(type = TcpServerSocketProperties.class, from = SocketsExtension.class)
@Xml(namespaceLocation = "http://www.mulesoft.org/schema/mule/httpn", namespace = "httpn")
@Export(resources = {"/META-INF/services/org/mule/runtime/core/config"}, classes = {HttpListenerConnectionManager.class})
public class HttpConnector
{

    public static final String URL_CONFIGURATION = "URL Configuration";
    public static final String URL_OVERRIDE_CONFIGURATION = "URL Override Configuration";
    public static final String API_CONFIGURATION = "API Configuration";
    public static final String TLS_CONFIGURATION = "TLS Configuration";
    public static final String OTHER_SETTINGS = "Other Settings";
    public static final String AUTHENTICATION = "Authentication";
    public static final String TLS = "TLS";
}
