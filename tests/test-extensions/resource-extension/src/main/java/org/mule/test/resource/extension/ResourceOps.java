/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.resource.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import jakarta.inject.Inject;

public class ResourceOps {

  @Inject
  private HttpService fakeHttpService;

  @Inject
  private ResourceLocator resourceLocator;

  /**
   * This operation loads MANIFEST files and gets their Bundle-Description or simply a resource as String. It allows testing which
   * resources will be accessible to a plugin classloader as well as the {@link ResourceLocator}.
   *
   * @param resource the resource to load
   * @return the String representation of the resource or Bundle-Description if it was a MANIFEST
   */
  @MediaType(TEXT_PLAIN)
  public String access(String resource) {
    Optional<InputStream> resourceStream = resourceLocator.load(resource, this);
    // We'll interpret MANIFEST files but also allow simple output to test other scenarios
    if (resource.endsWith(".MF")) {
      if (resourceStream.isPresent()) {
        Properties properties = new Properties();
        try {
          properties.load(resourceStream.get());
        } catch (IOException e) {
          throw new MuleRuntimeException(e);
        }
        return properties.getProperty("Bundle-Description");
      }
    }
    return resourceStream.isPresent() ? IOUtils.toString(resourceStream.get()) : "";

  }

  /**
   * This operation depends on the fake HTTP service to provide resource access through it and validate how the service
   * classloader behaves. It should only be used when the fake HTTP service is available.
   *
   * @param resource the MANIFEST resource to load
   * @return the Bundle-Description of the MANIFEST or an empty String if not found
   */
  @MediaType(TEXT_PLAIN)
  public String serviceAccess(String resource) {
    try {
      HttpRequestBuilder requestBuilder = HttpRequest.builder().uri("pepito.com");
      if (resource != null) {
        requestBuilder.entity(new ByteArrayHttpEntity(resource.getBytes()));
      }
      return IOUtils.toString(fakeHttpService.getClientFactory()
          .create(null)
          .send(requestBuilder.build())
          .getEntity().getContent());
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }
}
