/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.internal.dsl.DefaultDslResolvingContext;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SmallTest
public class ExtensionActivatorTestCase extends AbstractMuleTestCase {


  @Test
  public void enumsReleasedWhenStopped() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(TYPE_PROPERTY_NAME, HeisenbergExtension.class.getName());
    attributes.put(VERSION, "1.0.0");

    ExtensionModel extensionModel =
        new DefaultJavaExtensionModelLoader().loadExtensionModel(HeisenbergExtension.class.getClassLoader(),
                                                                 new DefaultDslResolvingContext(Collections.emptySet()),
                                                                 attributes);

    ExtensionActivator extensionActivator = new ExtensionActivator(mock(ExtensionErrorsRegistrant.class, RETURNS_DEEP_STUBS),
                                                                   mockMuleContext());
    extensionActivator.activateExtension(extensionModel);
    assertThat(extensionActivator.getEnumTypes().size(), is(greaterThan(0)));

    extensionActivator.stop();
    assertThat(extensionActivator.getEnumTypes(), hasSize(0));
  }
}
