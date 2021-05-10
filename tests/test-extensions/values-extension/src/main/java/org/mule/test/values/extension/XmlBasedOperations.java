/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.test.values.extension.metadata.XmlTypeResolver;
import org.mule.test.values.extension.resolver.WithRequiredParameterSdkValueProvider;

import java.io.InputStream;

public class XmlBasedOperations {

  public void textAsActingForAttributeValue(@FieldValues(value = WithRequiredParameterSdkValueProvider.class,
      targetSelectors = "nested.tag.@customAttribute", bindings = @Binding(actingParameter = "requiredValue",
          extractionExpression = "xmlBody.nested.tag.\"__text\"")) @Content @TypeResolver(XmlTypeResolver.class) InputStream xmlBody) {}

  public void attributeAsActingForTagContentValue(@FieldValues(value = WithRequiredParameterSdkValueProvider.class,
      targetSelectors = "nested.tag", bindings = @Binding(actingParameter = "requiredValue",
          extractionExpression = "xmlBody.nested.anotherTag.@customAttribute")) @Content @TypeResolver(XmlTypeResolver.class) InputStream xmlBody) {}

  public void tagContentAsActingForAttributeValue(@FieldValues(value = WithRequiredParameterSdkValueProvider.class,
      targetSelectors = "nested.tag.@customAttribute", bindings = @Binding(actingParameter = "requiredValue",
          extractionExpression = "xmlBody.nested.someTag")) @Content @TypeResolver(XmlTypeResolver.class) InputStream xmlBody) {}

}
