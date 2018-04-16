/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import java.io.InputStream;

import org.mule.api.annotation.NoImplement;

/**
 * Represents a generic resource provider, to be used instead of the artifact class loader.
 */
@NoImplement
public interface ResourceProvider {

  /**
   * @param uri location of the resource. It may be a classpath location or an absolute path.
   * @return the input stream of the resource or null if the resource was not found.
   */
  InputStream getResourceAsStream(String uri);
}
