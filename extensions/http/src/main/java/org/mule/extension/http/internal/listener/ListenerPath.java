/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import org.mule.runtime.core.util.StringUtils;

public class ListenerPath {

  private String basePath;
  private String resolvedPath;

  public ListenerPath(String basePath, String listenerPath) {
    this.basePath = basePath;
    this.resolvedPath = basePath == null ? listenerPath : basePath + listenerPath;
  }

  public String getResolvedPath() {
    return resolvedPath;
  }

  public String getRelativePath(String requestPath) {
    return basePath == null ? requestPath : requestPath.replace(basePath, StringUtils.EMPTY);
  }
}
