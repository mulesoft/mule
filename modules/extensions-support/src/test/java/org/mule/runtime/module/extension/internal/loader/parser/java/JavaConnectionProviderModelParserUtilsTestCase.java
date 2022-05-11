/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
