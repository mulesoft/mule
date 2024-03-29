/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("cached")
public class CLCachedConnectionProvider extends CLNoneConnectionProvider implements CachedConnectionProvider<String> {

  @Override
  public String getKind() {
    return "CACHED";
  }
}
