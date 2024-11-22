/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENCODING_SYSTEM_PROPERTY;

import static java.lang.System.getProperty;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.Charset.forName;

import org.mule.runtime.core.api.config.ArtifactEncoding;
import org.mule.runtime.core.api.config.MuleConfiguration;

import java.nio.charset.Charset;

import javax.inject.Inject;

public class DefaultArtifactEncoding implements ArtifactEncoding {

  @Inject
  private MuleConfiguration configuration;

  @Override
  public Charset getDefaultEncoding() {
    if (configuration != null && configuration.getDefaultEncoding() != null) {
      return forName(configuration.getDefaultEncoding());
    } else if (getProperty(MULE_ENCODING_SYSTEM_PROPERTY) != null) {
      return forName(getProperty(MULE_ENCODING_SYSTEM_PROPERTY));
    } else {
      return defaultCharset();
    }
  }

}
