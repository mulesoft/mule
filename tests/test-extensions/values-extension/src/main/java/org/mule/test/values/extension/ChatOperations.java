/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.test.values.extension.connection.ChatConnection;
import org.mule.test.values.extension.metadata.JsonTypeResolver;
import org.mule.test.values.extension.resolver.ChannelsValueProvider;
import org.mule.test.values.extension.resolver.ChatMultiLevelValueProvider;
import org.mule.test.values.extension.resolver.WorkspacesValueProvider;

import java.io.InputStream;

public class ChatOperations {

  public void withJsonBodyParameterWithField(@Connection ChatConnection chatConnection, @FieldValues(
      value = ChannelsValueProvider.class,
      targetSelectors = "channelId", bindings = {@Binding(actingParameter = "workspace",
          extractionExpression = "body.workspace")}) @FieldValues(
              value = WorkspacesValueProvider.class,
              targetSelectors = "workspace") @TypeResolver(JsonTypeResolver.class) @Content InputStream body) {}

  public void withJsonBodyParameterWithMultiLevelField(@Connection ChatConnection chatConnection, @FieldValues(
      value = ChatMultiLevelValueProvider.class,
      targetSelectors = {"workspace", "channelId"}) @TypeResolver(JsonTypeResolver.class) @Content InputStream body) {}

}
