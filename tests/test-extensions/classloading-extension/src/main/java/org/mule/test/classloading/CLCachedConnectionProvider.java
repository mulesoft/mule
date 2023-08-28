/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
