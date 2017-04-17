/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static org.mule.extension.http.internal.HttpConnectorConstants.CONFIGURATION_OVERRIDES;
import static org.mule.extension.http.internal.HttpConnectorConstants.OTHER_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.REQUEST_SETTINGS;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeSpaces;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.api.request.validator.SuccessStatusCodeValidator;
import org.mule.extension.http.internal.HttpRequestMetadataResolver;
import org.mule.extension.http.internal.HttpStreamingType;
import org.mule.extension.http.internal.request.client.HttpExtensionClient;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.service.http.api.HttpConstants;

import javax.inject.Inject;

public class HttpRequestOperations implements Initialisable, Disposable {

  private static final int WAIT_FOR_EVER = MAX_VALUE;

  @Inject
  private MuleContext muleContext;
  @Inject
  private SchedulerService schedulerService;

  private Scheduler scheduler;

  /**
   * Consumes an HTTP service.
   *
   * @param uriSettings URI settings parameter group
   * @param method The HTTP method for the request.
   * @param overrides configuration overrides parameter group
   * @param responseValidationSettings response validation parameter group
   * @param requestBuilder configures the request
   * @param outputSettings additional settings parameter group
   * @param client the http connection
   * @param config the configuration for this operation. All parameters not configured will be taken from it.
   * @return an {@link Result} with {@link HttpResponseAttributes}
   */
  @Summary("Executes a HTTP Request")
  @OutputResolver(output = HttpRequestMetadataResolver.class)
  @Throws(RequestErrorTypeProvider.class)
  @Streaming
  public void request(@Placement(order = 1) @ParameterGroup(name = "URI Settings") UriSettings uriSettings,
                      @Placement(order = 2) @Optional(defaultValue = "GET") String method,
                      @ParameterGroup(name = CONFIGURATION_OVERRIDES) ConfigurationOverrides overrides,
                      @ParameterGroup(
                          name = "Response Validation Settings") ResponseValidationSettings responseValidationSettings,
                      @Placement(order = 3) @ParameterGroup(name = REQUEST_SETTINGS) HttpRequesterRequestBuilder requestBuilder,
                      @ParameterGroup(name = OTHER_SETTINGS) OutputSettings outputSettings,
                      @Connection HttpExtensionClient client,
                      @Config HttpRequesterConfig config,
                      CompletionCallback<Object, HttpResponseAttributes> callback) {
    try {
      HttpRequesterRequestBuilder resolvedBuilder = requestBuilder != null ? requestBuilder : new HttpRequesterRequestBuilder();

      String resolvedUri;
      if (uriSettings.getUrl() == null) {
        UriParameters uriParameters = client.getDefaultUriParameters();
        String resolvedBasePath = config.getBasePath();
        String resolvedPath = resolvedBuilder.replaceUriParams(buildPath(resolvedBasePath, uriSettings.getPath()));
        resolvedUri = resolveUri(uriParameters.getScheme(), uriParameters.getHost(), uriParameters.getPort(), resolvedPath);
      } else {
        resolvedUri = resolvedBuilder.replaceUriParams(uriSettings.getUrl());
      }

      Boolean resolvedFollowRedirects =
          resolveIfNecessary(overrides.getFollowRedirects(), config.getFollowRedirects());
      HttpStreamingType resolvedStreamingMode =
          resolveIfNecessary(overrides.getRequestStreamingMode(), config.getRequestStreamingMode());
      HttpSendBodyMode resolvedSendBody = resolveIfNecessary(overrides.getSendBodyMode(), config.getSendBodyMode());
      Boolean resolvedParseResponse = resolveIfNecessary(overrides.getParseResponse(), config.getParseResponse());
      Integer resolvedTimeout = resolveResponseTimeout(config, overrides.getResponseTimeout());
      ResponseValidator responseValidator = responseValidationSettings.getResponseValidator();
      responseValidator = responseValidator != null ? responseValidator : new SuccessStatusCodeValidator("0..399");


      HttpRequester requester =
          new HttpRequester.Builder().setUri(resolvedUri).setMethod(method).setFollowRedirects(resolvedFollowRedirects)
              .setRequestStreamingMode(resolvedStreamingMode).setSendBodyMode(resolvedSendBody)
              .setAuthentication(client.getDefaultAuthentication()).setParseResponse(resolvedParseResponse)
              .setResponseTimeout(resolvedTimeout).setResponseValidator(responseValidator).setConfig(config)
              .setTransformationService(muleContext.getTransformationService()).setScheduler(scheduler)
              .build();

      requester.doRequest(client, resolvedBuilder, true, muleContext, callback);
    } catch (Exception e) {
      callback.error(e);
    }
  }

  private <T> T resolveIfNecessary(T value, T configValue) {
    return value != null ? value : configValue;
  }

  private String resolveUri(HttpConstants.Protocols scheme, String host, Integer port, String path) {
    // Encode spaces to generate a valid HTTP request.
    return format("%s://%s:%s%s", scheme.getScheme(), host, port, encodeSpaces(path));
  }

  private int resolveResponseTimeout(HttpRequesterConfig config, Integer responseTimeout) {
    if (responseTimeout == null && config.getResponseTimeout() != null) {
      responseTimeout = config.getResponseTimeout();
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

  @Override
  public void initialise() throws InitialisationException {
    this.scheduler = schedulerService.ioScheduler();
  }

  @Override
  public void dispose() {
    if (this.scheduler != null) {
      scheduler.shutdownNow();
    }
  }
}
