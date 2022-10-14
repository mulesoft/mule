/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.junit.Test;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.parser.java.CustomStaticTypeUtilsTestCase;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeWrapperTestCase {

  @Test
  public void enumParameterType() {
    TypeWrapper type = new TypeWrapper(CustomStaticTypeUtilsTestCase.TestEnum.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    for (ExtensionParameter parameter : type.getMethod("compareTo", Enum.class).map(WithParameters::getParameters)
        .orElse(emptyList())) {
      assertThat(parameter.getType().getTypeName(), is(("java.lang.Enum")));
    }
  }
}
