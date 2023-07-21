/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils;

import org.junit.Test;

public class JavaConnectionProviderModelParserUtilsTestCase {

  @Test
  public void isTransactionalLegacyApi() {
    assertThat(JavaConnectionProviderModelParserUtils
        .isTransactional(new TypeWrapper(JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class,
                                         TYPE_LOADER)),
               is(true));
  }

  @Test
  public void isTransactionalSdkApi() {
    assertThat(JavaConnectionProviderModelParserUtils
        .isTransactional(new TypeWrapper(JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection.class,
                                         TYPE_LOADER)),
               is(true));
  }

}
