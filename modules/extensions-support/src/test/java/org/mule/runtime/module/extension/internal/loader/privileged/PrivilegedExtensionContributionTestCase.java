/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.privileged;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.module.extension.internal.util.extension.privileged.ChangeNameDeclarationEnricher.NEW_NAME;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.module.extension.internal.util.extension.privileged.PrivilegedExtension;

import org.junit.Test;

public class PrivilegedExtensionContributionTestCase extends AbstractMuleTestCase {

  @Test
  public void loadPrivilegedExtension() {
    ExtensionModel extension = loadExtension(PrivilegedExtension.class);
    assertThat(extension.getName(), is(NEW_NAME));
  }
}
