/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.runtime.core.api.type.catalog.DefaultListableTypeLoadersRepository.from;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultListableTypeLoadersRepositoryTestCase {

  private DefaultListableTypeLoadersRepository repository;

  @Rule
  public ExpectedException expected = none();

  @Before
  public void setUp() {
    ExtensionModel presentExtensionModel = mockExtensionModel("present");
    ExtensionModel alsoPresentExtensionModel = mockExtensionModel("also-present");
    repository = from(asList(presentExtensionModel, alsoPresentExtensionModel));
  }

  private static ExtensionModel mockExtensionModel(String prefix) {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class);
    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(prefix).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);
    return mockExtensionModel;
  }

  @Test
  public void notPresentTypeLoaderThrows() throws MuleException {
    expected.expect(MuleException.class);
    expected.expectMessage("Type loader not found for prefix 'not-present'. Available prefixes are: [present, also-present]");
    repository.getTypeLoaderByPrefix("not-present");
  }

  @Test
  public void presentTypeLoaderIsRetrieved() throws MuleException {
    assertThat(repository.getTypeLoaderByPrefix("present"), is(notNullValue()));
  }

  @Test
  public void getAllLoaders() {
    assertThat(repository.getAllTypeLoaders().keySet(), containsInAnyOrder("present", "also-present"));
  }
}
