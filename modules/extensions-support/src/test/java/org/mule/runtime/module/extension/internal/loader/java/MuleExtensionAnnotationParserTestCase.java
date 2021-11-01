/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;
import org.mule.test.module.extension.internal.util.extension.SimpleExportedType;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingLegacyApi;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingSdkApi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MuleExtensionAnnotationParserTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  @Test
  public void getExtensionInfoFromExtensionUsingTheSdkApi() {
    ExtensionInfo extensionInfo = MuleExtensionAnnotationParser.getExtensionInfo(SimpleExtensionUsingSdkApi.class);

    assertThat(extensionInfo.getName(), is("SimpleExtension"));
    assertThat(extensionInfo.getVendor(), is("Mulesoft"));
    assertThat(extensionInfo.getCategory(), equalTo(COMMUNITY));
  }

  @Test
  public void getExtensionInfoFromExtensionUsingTheLegacyApi() {
    ExtensionInfo extensionInfo = MuleExtensionAnnotationParser.getExtensionInfo(SimpleExtensionUsingLegacyApi.class);

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

    MuleExtensionAnnotationParser.getExtensionInfo(SimpleExportedType.class);
  }

}
