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

import java.util.Set;

import org.junit.Test;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.tck.junit4.matcher.ValueMatcher;

public class XmlBasedOperationValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/xml-based-operation-values.xml";
  }

  @Test
  public void textAsActingForAttributeValue() throws Exception {
    ValueResult result = getValueResult("textAsActingForAttributeValue", "xmlBody", "nested.tag.@customAttribute");
    assertThat(result.getValues(), hasSize(1));
    assertThat(result.getValues(), hasValues("Acting parameter value"));
  }

  @Test
  public void attributeAsActingForTagContentValue() throws Exception {
    ValueResult result = getValueResult("attributeAsActingForTagContentValue", "xmlBody", "nested.tag");
    assertThat(result.getValues(), hasSize(1));
    assertThat(result.getValues(), hasValues("Acting parameter value"));
  }

  @Test
  public void tagContentAsActingForAttributeValue() throws Exception {
    ValueResult result = getValueResult("tagContentAsActingForAttributeValue", "xmlBody", "nested.tag.@customAttribute");
    assertThat(result.getValues(), hasSize(1));
    assertThat(result.getValues(), hasValues("Acting parameter value"));
  }

}
