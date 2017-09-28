/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import java.io.InputStream;

/**
 * Represents a generic resource provider, to be used instead of the artifact class loader.
 */
public interface ResourceProvider {

  InputStream getResourceAsStream(String uri);
}
