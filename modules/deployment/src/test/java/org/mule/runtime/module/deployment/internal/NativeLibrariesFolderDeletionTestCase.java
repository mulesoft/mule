/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletion;
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
    NativeLibrariesFolderDeletion nativeLibrariesFolderDeletion =
        new NativeLibrariesFolderDeletion(ARTIFACT_ID, nativeLibrariesFolder.getRoot());

    assertTrue(nativeLibrariesFolder.getRoot().exists());
    assertTrue(nativeLibrariesFolderDeletion.doAction());
    assertFalse(nativeLibrariesFolder.getRoot().exists());
  }
}
