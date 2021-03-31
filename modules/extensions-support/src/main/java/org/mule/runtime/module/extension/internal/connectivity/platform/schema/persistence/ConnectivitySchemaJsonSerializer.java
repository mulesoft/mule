/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema.persistence;

import org.mule.runtime.module.extension.internal.connectivity.platform.schema.ConnectivitySchema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConnectivitySchemaJsonSerializer {

  private Gson gson;

  public ConnectivitySchemaJsonSerializer(boolean prettyPrint) {
    GsonBuilder builder = new GsonBuilder();
    if (prettyPrint) {
      builder.setPrettyPrinting();
    }

    gson = builder.create();
  }

  public String serialize(ConnectivitySchema schema) {
    return gson.toJson(schema);
  }

  public ConnectivitySchema deserialize(String json) {
    return gson.fromJson(json, ConnectivitySchema.class);
  }
}
