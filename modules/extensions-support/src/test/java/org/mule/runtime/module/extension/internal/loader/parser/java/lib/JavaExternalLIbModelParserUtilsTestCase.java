/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.lib;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.meta.ExternalLibraryType.NATIVE;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.ExternalLib;
import org.mule.sdk.api.meta.ExternalLibraryType;
import org.mule.test.module.extension.internal.util.extension.SimpleExportedType;

import java.util.List;

import org.junit.Test;

public class JavaExternalLIbModelParserUtilsTestCase {

  @Test
  public void getExternalLibraryFromExtensionUsingTheSdkApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);

    ExtensionTypeWrapper<SimpleExtensionUsingSdkApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingSdkApi.class, typeLoader);

    List<ExternalLibraryModel> externalLibraryModelList =
        JavaExternalLibModelParserUtils.parseExternalLibraryModels(extensionTypeWrapper);

    assertThat(externalLibraryModelList.size(), is(1));
    assertThat(externalLibraryModelList.get(0).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(0).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(0).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(0).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(0).getType(), equalTo(NATIVE));
  }

  @Test
  public void getExternalLibraryFromExtensionUsingTheLegacyApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);

    ExtensionTypeWrapper<SimpleExtensionUsingLegacyApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingLegacyApi.class, typeLoader);

    List<ExternalLibraryModel> externalLibraryModelList =
        JavaExternalLibModelParserUtils.parseExternalLibraryModels(extensionTypeWrapper);

    assertThat(externalLibraryModelList.size(), is(1));
    assertThat(externalLibraryModelList.get(0).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(0).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(0).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(0).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(0).getType(), equalTo(NATIVE));
  }

  @Test
  public void getExternalLibraryFromExtensionUsingBothTheLegacyAndTheSdkApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);

    ExtensionTypeWrapper<SimpleMixedApiExtension> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleMixedApiExtension.class, typeLoader);

    List<ExternalLibraryModel> externalLibraryModelList =
        JavaExternalLibModelParserUtils.parseExternalLibraryModels(extensionTypeWrapper);

    assertThat(externalLibraryModelList.size(), is(2));
    assertThat(externalLibraryModelList.get(0).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(0).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(0).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(0).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(0).getType(), equalTo(NATIVE));
    assertThat(externalLibraryModelList.get(1).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(1).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(1).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(1).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(1).getType(), equalTo(NATIVE));
  }

  @Test
  public void getExternalLibraryFromExtensionRepeatingheLegacyApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);

    ExtensionTypeWrapper<SimpleRepeatingApiExtension> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleRepeatingApiExtension.class, typeLoader);

    List<ExternalLibraryModel> externalLibraryModelList =
        JavaExternalLibModelParserUtils.parseExternalLibraryModels(extensionTypeWrapper);

    assertThat(externalLibraryModelList.size(), is(3));
    assertThat(externalLibraryModelList.get(0).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(0).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(0).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(0).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(0).getType(), equalTo(NATIVE));
    assertThat(externalLibraryModelList.get(1).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(1).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(1).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(1).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(1).getType(), equalTo(NATIVE));
    assertThat(externalLibraryModelList.get(2).getName(), is(EXTERNAL_LIB_NAME));
    assertThat(externalLibraryModelList.get(2).getDescription(), is(EXTERNAL_LIB_DESCRIPTION));
    assertThat(externalLibraryModelList.get(2).getRegexMatcher().get(), is(EXTERNAL_LIB_FILE_NAME));
    assertThat(externalLibraryModelList.get(2).getRequiredClassName().get(), is(EXTERNAL_LIB_CLASS_NAME));
    assertThat(externalLibraryModelList.get(2).getType(), equalTo(NATIVE));
  }


  public static final String EXTERNAL_LIB_NAME = "SimpleExtension.so";
  public static final String EXTERNAL_LIB_DESCRIPTION = "Cool lib description";
  public static final String EXTERNAL_LIB_FILE_NAME = "SimpleExtension.so";
  public static final String EXTERNAL_LIB_CLASS_NAME = "org.java.Main";

  @Extension(name = "SimpleExtension")
  @Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
  @ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME, requiredClassName = EXTERNAL_LIB_CLASS_NAME,
      type = ExternalLibraryType.NATIVE)
  private static class SimpleExtensionUsingSdkApi {
  }

  @Extension(name = "SimpleExtension")
  @Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
  @org.mule.runtime.extension.api.annotation.ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME, requiredClassName = EXTERNAL_LIB_CLASS_NAME,
      type = org.mule.runtime.api.meta.ExternalLibraryType.NATIVE)
  private static class SimpleExtensionUsingLegacyApi {
  }

  @Extension(name = "SimpleExtension")
  @Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
  @ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME,
      requiredClassName = EXTERNAL_LIB_CLASS_NAME, type = ExternalLibraryType.NATIVE)
  @org.mule.runtime.extension.api.annotation.ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME, requiredClassName = EXTERNAL_LIB_CLASS_NAME,
      type = org.mule.runtime.api.meta.ExternalLibraryType.NATIVE)
  private static class SimpleMixedApiExtension {
  }

  @Extension(name = "SimpleExtension")
  @Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
  @ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME,
      requiredClassName = EXTERNAL_LIB_CLASS_NAME, type = ExternalLibraryType.NATIVE)
  @ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME,
      requiredClassName = EXTERNAL_LIB_CLASS_NAME, type = ExternalLibraryType.NATIVE)
  @org.mule.runtime.extension.api.annotation.ExternalLib(name = EXTERNAL_LIB_NAME, description = EXTERNAL_LIB_DESCRIPTION,
      nameRegexpMatcher = EXTERNAL_LIB_FILE_NAME, requiredClassName = EXTERNAL_LIB_CLASS_NAME,
      type = org.mule.runtime.api.meta.ExternalLibraryType.NATIVE)
  private static class SimpleRepeatingApiExtension {
  }
}
