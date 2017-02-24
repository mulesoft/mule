/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.temporary;

import static org.mule.extension.http.internal.listener.HttpListener.HTTP_NAMESPACE;
import org.mule.extension.http.api.error.HttpError;
import org.mule.extension.http.api.request.authentication.BasicAuthentication;
import org.mule.extension.http.api.request.authentication.DigestAuthentication;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.authentication.NtlmAuthentication;
import org.mule.extension.http.api.request.proxy.DefaultNtlmProxyConfig;
import org.mule.extension.http.api.request.proxy.DefaultProxyConfig;
import org.mule.extension.http.api.request.validator.FailureStatusCodeValidator;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.api.request.validator.SuccessStatusCodeValidator;
import org.mule.extension.http.internal.HttpOperations;
import org.mule.extension.http.internal.listener.server.HttpListenerConfig;
import org.mule.extension.http.internal.request.HttpRequesterConfig;
import org.mule.extension.http.internal.request.client.HttpExtensionClient;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.service.http.api.client.proxy.ProxyConfig;

/**
 * HTTP connector used to handle and perform HTTP requests.
 * <p>
 * This class only serves as an extension definition. It's configurations are divided on server ({@link HttpListenerConfig}) and
 * client ({@link HttpRequesterConfig}) capabilities.
 *
 * @since 4.0
 */
@Extension(name = "HTTP", description = "Connector to handle and perform HTTP requests")
@Configurations({HttpListenerConfig.class, HttpRequesterConfig.class})
@Operations(HttpOperations.class)
@SubTypeMapping(baseType = HttpAuthentication.class,
    subTypes = {BasicAuthentication.class, DigestAuthentication.class, NtlmAuthentication.class})
@SubTypeMapping(baseType = ProxyConfig.class, subTypes = {DefaultProxyConfig.class, DefaultNtlmProxyConfig.class})
@SubTypeMapping(baseType = ResponseValidator.class,
    subTypes = {SuccessStatusCodeValidator.class, FailureStatusCodeValidator.class})
@Import(type = TcpClientSocketProperties.class, from = "Sockets")
@Import(type = TcpServerSocketProperties.class, from = "Sockets")
@ErrorTypes(HttpError.class)
@Xml(namespace = "http://www.mulesoft.org/schema/mule/httpn", prefix = HTTP_NAMESPACE)
//TODO move back to package org.mule.extension.http.internal as part of MULE-10651. Now we are using this package
//because it doesn't work in the former package since the classloader mechanism will try to load the class from another bundle.
@Export(classes = {HttpExtensionClient.class})
public class HttpConnector {

}

