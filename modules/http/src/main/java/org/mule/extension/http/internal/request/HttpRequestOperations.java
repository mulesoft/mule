/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.Integer.MAX_VALUE;
import static org.mule.extension.http.internal.HttpConnectorConstants.CONFIGURATION_OVERRIDES;
import static org.mule.extension.http.internal.HttpConnectorConstants.OTHER_SETTINGS;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import org.mule.extension.http.api.HttpMetadataKey;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.api.request.validator.SuccessStatusCodeValidator;
import org.mule.extension.http.internal.HttpRequestMetadataResolver;
import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.internal.HttpParser;

import java.util.function.Function;

import javax.inject.Inject;


public class HttpRequestOperations {

  private static final int WAIT_FOR_EVER = MAX_VALUE;

  @Inject
  private MuleContext muleContext;

  /**
   * Consumes an HTTP service.
   *
   * @param path Path where the request will be sent.
   * @param method The HTTP method for the request.
   * @param host Host where the requests will be sent.
   * @param port Port where the requests will be sent.
   * @param source The expression used to obtain the body that will be sent in the request. Default is empty, so the payload will
   *        be used as the body.
   * @param followRedirects Specifies whether to follow redirects or not.
   * @param parseResponse Defines if the HTTP response should be parsed or it's raw contents should be propagated instead.
   * @param requestStreamingMode Defines if the request should be sent using streaming or not.
   * @param sendBodyMode Defines if the request should contain a body or not.
   * @param responseTimeout Maximum time that the request element will block the execution of the flow waiting for the HTTP
   *        response.
   * @param responseValidator Configures error handling of the response.
   * @param config the {@link HttpConnector} configuration for this operation. All parameters not configured will be taken from
   *        it.
   * @param muleEvent the current {@link Event}
   * @return an {@link Result} with {@link HttpResponseAttributes}
   */
  @Summary("Executes a HTTP Request")
  @OutputResolver(output = HttpRequestMetadataResolver.class)
  public Result<Object, HttpResponseAttributes> request(String path, @Optional(defaultValue = "GET") String method,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = "Response Validation Settings") ResponseValidator responseValidator,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES, order = 1) String host,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES, order = 2) Integer port,
                                                        @Optional String source,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES,
                                                            order = 3) Boolean followRedirects,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES,
                                                            order = 4) HttpSendBodyMode sendBodyMode,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES,
                                                            order = 5) HttpStreamingType requestStreamingMode,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES,
                                                            order = 6) Boolean parseResponse,
                                                        @Optional @Placement(tab = ADVANCED,
                                                            group = CONFIGURATION_OVERRIDES,
                                                            order = 7) Integer responseTimeout,
                                                        @Optional HttpRequesterRequestBuilder requestBuilder,
                                                        @MetadataKeyId @Optional(
                                                            defaultValue = "ANY") @Placement(
                                                                tab = ADVANCED,
                                                                group = OTHER_SETTINGS) HttpMetadataKey outputType,
                                                        @Connection HttpClient client,
                                                        @UseConfig HttpRequesterConfig config, Event muleEvent)
      throws MuleException {
    HttpRequesterRequestBuilder resolvedBuilder = requestBuilder != null ? requestBuilder : new HttpRequesterRequestBuilder();
    UriParameters uriParameters = client.getDefaultUriParameters();

    String resolvedHost = resolveIfNecessary(host, uriParameters.getHost(), muleEvent);
    Integer resolvedPort = resolveIfNecessary(port, uriParameters.getPort(), muleEvent);
    String resolvedBasePath = config.getBasePath().apply(muleEvent);
    String resolvedPath = resolvedBuilder.replaceUriParams(buildPath(resolvedBasePath, path));

    String resolvedUri = resolveUri(uriParameters.getScheme(), resolvedHost, resolvedPort, resolvedPath);
    Boolean resolvedFollowRedirects = resolveIfNecessary(followRedirects, config.getFollowRedirects(), muleEvent);
    HttpStreamingType resolvedStreamingMode =
        resolveIfNecessary(requestStreamingMode, config.getRequestStreamingMode(), muleEvent);
    HttpSendBodyMode resolvedSendBody = resolveIfNecessary(sendBodyMode, config.getSendBodyMode(), muleEvent);
    Boolean resolvedParseResponse = resolveIfNecessary(parseResponse, config.getParseResponse(), muleEvent);
    Integer resolvedTimeout = resolveResponseTimeout(muleEvent, config, responseTimeout);
    ResponseValidator resolvedValidator =
        responseValidator != null ? responseValidator : new SuccessStatusCodeValidator("0..399");

    HttpRequester requester =
        new HttpRequester.Builder().setUri(resolvedUri).setMethod(method).setFollowRedirects(resolvedFollowRedirects)
            .setRequestStreamingMode(resolvedStreamingMode).setSendBodyMode(resolvedSendBody).setSource(source)
            .setAuthentication(client.getDefaultAuthentication()).setParseResponse(resolvedParseResponse)
            .setResponseTimeout(resolvedTimeout).setResponseValidator(resolvedValidator).setConfig(config).build();

    // TODO MULE-10340 See how the flowConstruct calling this operation can be retrieved
    final Flow flowConstruct = new Flow("httpRequestOperation", muleContext);
    return Result.<Object, HttpResponseAttributes>builder(requester.doRequest(muleEvent, client, resolvedBuilder, true,
                                                                              muleContext, flowConstruct))
        .build();
  }

  private <T> T resolveIfNecessary(T value, Function<Event, T> function, Event event) {
    return value != null ? value : function.apply(event);
  }

  private String resolveUri(HttpConstants.Protocols scheme, String host, Integer port, String path) {
    // Encode spaces to generate a valid HTTP request.
    String resolvedPath = HttpParser.encodeSpaces(path);

    return String.format("%s://%s:%s%s", scheme.getScheme(), host, port, resolvedPath);
  }

  private int resolveResponseTimeout(Event muleEvent, HttpRequesterConfig config, Integer responseTimeout) {
    if (responseTimeout == null && config.getResponseTimeout() != null) {
      responseTimeout = config.getResponseTimeout().apply(muleEvent);
    }

    if (muleContext.getConfiguration().isDisableTimeouts()) {
      return WAIT_FOR_EVER;
    } else {
      return responseTimeout != null ? responseTimeout : muleContext.getConfiguration().getDefaultResponseTimeout();
    }
  }

  protected String buildPath(String basePath, String path) {
    String resolvedBasePath = basePath;
    String resolvedRequestPath = path;

    if (!resolvedBasePath.startsWith("/")) {
      resolvedBasePath = "/" + resolvedBasePath;
    }

    if (resolvedBasePath.endsWith("/") && resolvedRequestPath.startsWith("/")) {
      resolvedBasePath = resolvedBasePath.substring(0, resolvedBasePath.length() - 1);
    }

    if (!resolvedBasePath.endsWith("/") && !resolvedRequestPath.startsWith("/") && !resolvedRequestPath.isEmpty()) {
      resolvedBasePath += "/";
    }

    return resolvedBasePath + resolvedRequestPath;

  }
}
