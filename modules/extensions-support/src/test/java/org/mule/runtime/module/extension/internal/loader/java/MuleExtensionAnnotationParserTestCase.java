/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.getExtensionInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceAnnotation;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.sdk.api.annotation.Extension;
import org.mule.test.module.extension.internal.util.extension.SimpleExportedType;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingLegacyApi;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingSdkApi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MuleExtensionAnnotationParserTestCase {

  private static final String LEGACY_EXTENSION_NAME = "legacyExtensionName";
  private static final String SDK_EXTENSION_NAME = "sdkExtensionName";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getExtensionInfoFromExtensionUsingTheSdkApi() {
    ExtensionInfo extensionInfo = getExtensionInfo(SimpleExtensionUsingSdkApi.class);

    assertThat(extensionInfo.getName(), is("SimpleExtension"));
    assertThat(extensionInfo.getVendor(), is("Mulesoft"));
    assertThat(extensionInfo.getCategory(), equalTo(COMMUNITY));
  }

  @Test
  public void getExtensionInfoFromExtensionUsingTheLegacyApi() {
    ExtensionInfo extensionInfo = getExtensionInfo(SimpleExtensionUsingLegacyApi.class);

    assertThat(extensionInfo.getName(), is("SimpleExtension"));
    assertThat(extensionInfo.getVendor(), is("Mulesoft"));
    assertThat(extensionInfo.getCategory(), equalTo(COMMUNITY));
  }

  @Test
  public void getExtensionInfoFromExtensionNotUsingTheExtensionAnnotation() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage(containsString("Class 'org.mule.test.module.extension.internal.util.extension.SimpleExportedType' " +
            "not annotated with neither 'org.mule.runtime.extension.api.annotation.Extension' nor 'org.mule.sdk.api.annotation.Extension'"));

    getExtensionInfo(SimpleExportedType.class);
  }

  @Test
  public void mapReduceAnnotationInHierarchy() {
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    String extensionName = mapReduceAnnotation(new TypeWrapper(AnnotatedClass.class, typeLoader),
                                               org.mule.runtime.extension.api.annotation.Extension.class,
                                               Extension.class,
                                               ann -> ann
                                                   .getStringValue(org.mule.runtime.extension.api.annotation.Extension::name),
                                               ann -> ann.getStringValue(Extension::name),
                                               () -> new IllegalModelDefinitionException("oops"))
        .orElse(null);

    assertThat(extensionName, equalTo(SDK_EXTENSION_NAME));

    extensionName = mapReduceAnnotation(new TypeWrapper(LegacyAnnotatedClass.class, typeLoader),
                                        org.mule.runtime.extension.api.annotation.Extension.class,
                                        Extension.class,
                                        ann -> ann.getStringValue(org.mule.runtime.extension.api.annotation.Extension::name),
                                        ann -> ann.getStringValue(Extension::name),
                                        () -> new IllegalModelDefinitionException("oops"))
        .orElse(null);

    assertThat(extensionName, equalTo(LEGACY_EXTENSION_NAME));
  }

  @Extension(name = SDK_EXTENSION_NAME)
  private static class BaseAnnotatedClass {

  }

  @Extension(name = LEGACY_EXTENSION_NAME)
  private static class LegacyBaseAnnotatedClass {

  }

  private static class AnnotatedClass extends BaseAnnotatedClass {

  }

  private static class LegacyAnnotatedClass extends LegacyBaseAnnotatedClass {

  }
}
