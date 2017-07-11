/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.loader.validation.PrivilegedApiValidator.NO_PRIVILEGED_ARTIFACTS_ERROR;
import static org.mule.runtime.module.extension.internal.loader.validation.PrivilegedApiValidator.NO_PRIVILEGED_PACKAGES_ERROR;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PrivilegedApiValidatorTestCase extends AbstractMuleTestCase {

  private static final String EXTENSION_NAME = "my extension";

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
  }

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  private PrivilegedApiValidator validator = new PrivilegedApiValidator();

  @Test
  public void valid() {
    when(extensionModel.getPrivilegedArtifacts()).thenReturn(singleton("org.mule.test:foo"));
    when(extensionModel.getPrivilegedPackages()).thenReturn(singleton("org.foo"));
    validate(extensionModel, validator);
  }

  @Test
  public void noPrivilegedExportedPackages() {
    when(extensionModel.getPrivilegedPackages()).thenReturn(emptySet());
    when(extensionModel.getPrivilegedArtifacts()).thenReturn(singleton("org.mule.test:foo"));

    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(containsString(NO_PRIVILEGED_PACKAGES_ERROR));

    validate(extensionModel, validator);
  }

  @Test
  public void noPrivilegedArtifacts() {
    when(extensionModel.getPrivilegedPackages()).thenReturn(singleton("org.foo"));
    when(extensionModel.getPrivilegedArtifacts()).thenReturn(emptySet());

    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(containsString(NO_PRIVILEGED_ARTIFACTS_ERROR));

    validate(extensionModel, validator);
  }

  @Test
  public void noPrivilegedApi() throws Exception {
    when(extensionModel.getPrivilegedPackages()).thenReturn(emptySet());
    when(extensionModel.getPrivilegedArtifacts()).thenReturn(emptySet());
    validate(extensionModel, validator);
  }
}
