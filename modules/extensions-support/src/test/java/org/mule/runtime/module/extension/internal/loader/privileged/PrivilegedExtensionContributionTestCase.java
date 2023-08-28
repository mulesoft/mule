/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.privileged;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.runtime.module.extension.internal.loader.privileged.extension.ChangeNameDeclarationEnricher.NEW_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.extension.internal.loader.privileged.extension.PrivilegedExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class PrivilegedExtensionContributionTestCase extends AbstractMuleTestCase {

  @Test
  public void loadPrivilegedExtension() {
    ExtensionModel extension = loadExtension(PrivilegedExtension.class);
    assertThat(extension.getName(), is(NEW_NAME));
  }
}
