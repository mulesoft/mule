/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.tck.junit4.matcher.ValueMatcher;

import java.util.Set;

import org.junit.Test;

public class ChatOperationValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/chat-operation-values.xml";
  }

  @Test
  public void workspaceProvider() throws Exception {
    ValueResult result = getValueResult("workspaceValueProvider", "body", "workspace");
    assertThat(result.getValues(), hasSize(3));
    assertThat(result.getValues(), hasValues("workspace1", "workspace2", "workspace3"));
  }

  @Test
  public void channelIdProvider() throws Exception {
    ValueResult result = getValueResult("channelIdValueProvider", "body", "channelId");
    assertThat(result.getValues(), hasSize(2));
    assertThat(result.getValues(), hasValues("channel for workspace2", "other channel for workspace2"));
  }

  @Test
  public void workspaceProviderWithExpression() throws Exception {
    ValueResult result = getValueResult("workspaceValueProviderWithExpression", "body", "workspace");
    assertThat(result.getValues(), hasSize(3));
    assertThat(result.getValues(), hasValues("workspace1", "workspace2", "workspace3"));
  }

  @Test
  public void channelIdProviderWithExpression() throws Exception {
    ValueResult result = getValueResult("channelIdValueProviderWithExpression", "body", "channelId");
    assertThat(result.getValues(), hasSize(2));
    assertThat(result.getValues(), hasValues("channel for workspace2", "other channel for workspace2"));
  }

  @Test
  public void parameterWithMultiLevelFieldValues() throws Exception {
    Set<Value> values = getValues("multiLevelValueProvider", "body", "workspace");

    ValueMatcher workspace1Value = valueWithId("workspace1")
        .withDisplayName("workspace1")
        .withPartName("body.workspace")
        .withChilds(valueWithId("one channel")
            .withDisplayName("one channel")
            .withPartName("body.channelId"),
                    valueWithId("another channel")
                        .withDisplayName("another channel")
                        .withPartName("body.channelId"),
                    valueWithId("last channel channel")
                        .withDisplayName("last channel channel")
                        .withPartName("body.channelId"));

    ValueMatcher workspace2Value = valueWithId("workspace2")
        .withDisplayName("workspace2")
        .withPartName("body.workspace")
        .withChilds(valueWithId("channel for workspace2")
            .withDisplayName("channel for workspace2")
            .withPartName("body.channelId"),
                    valueWithId("other channel for workspace2")
                        .withDisplayName("other channel for workspace2")
                        .withPartName("body.channelId"));

    ValueMatcher workspace3Value = valueWithId("workspace3")
        .withDisplayName("workspace3")
        .withPartName("body.workspace")
        .withChilds(valueWithId("only channel for workspace3")
            .withDisplayName("only channel for workspace3")
            .withPartName("body.channelId"));

    assertThat(values, hasValues(workspace1Value, workspace2Value, workspace3Value));
  }

}
