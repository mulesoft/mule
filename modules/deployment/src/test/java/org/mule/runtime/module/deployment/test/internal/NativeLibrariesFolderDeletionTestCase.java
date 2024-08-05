/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.module.deployment.internal.ActionTask;
import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletionActionTask;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Issue("W-15894519")
public class NativeLibrariesFolderDeletionTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_ID = "application-test";

  @Rule
  public TemporaryFolder nativeLibrariesFolder = new TemporaryFolder();

  @Test
  public void nativeLibrariesFolderDeletionDeletesTheAppNativeLibrariesFolderWhenExistsAndReturnsTrue() {
    ActionTask nativeLibrariesFolderDeletionActionTask =
        new NativeLibrariesFolderDeletionActionTask(ARTIFACT_ID, nativeLibrariesFolder.getRoot());

    assertTrue(nativeLibrariesFolder.getRoot().exists());
    assertTrue(nativeLibrariesFolderDeletionActionTask.tryAction());
    assertFalse(nativeLibrariesFolder.getRoot().exists());
  }
}
