/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.provided.dependency.ProvidedDependencyExtension;

import org.junit.Test;

public class ExtensionWithProvidedDependencyTestCase {

  @Test
  public void extensionWithParametersRelyingOnProvidedDependencyTypesIsLoaded() {
    ExtensionModel extension = loadExtension(ProvidedDependencyExtension.class);

    assertThat(extension.getName(), is(ProvidedDependencyExtension.NAME));
    assertThat(extension.getMinMuleVersion().isPresent(), is(true));
    assertThat(extension.getMinMuleVersion().get().toString(), is("4.1.1"));
  }
}
