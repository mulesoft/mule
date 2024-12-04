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

import org.mule.runtime.api.config.ArtifactEncoding;

import java.nio.charset.Charset;

public class DefaultArtifactEncoding implements ArtifactEncoding {

  private Charset defaultCharset;

  public DefaultArtifactEncoding(String defaultEncoding) {
    if (defaultEncoding != null) {
      defaultCharset = forName(defaultEncoding);
    } else if (getProperty(MULE_ENCODING_SYSTEM_PROPERTY) != null) {
      defaultCharset = forName(getProperty(MULE_ENCODING_SYSTEM_PROPERTY));
    } else {
      defaultCharset = defaultCharset();
    }
  }

  @Override
  public Charset getDefaultEncoding() {
    return defaultCharset;
  }

}
