/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.FileUtils.openDirectory;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PersistentObjectStorePartitionTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder objectStoreFolder = new TemporaryFolder();

  private MuleContext muleContext = mockMuleContext();

  @Mock
  private MuleConfiguration muleConfiguration;

  private File workingDirectory;

  private PersistentObjectStorePartition partition;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    workingDirectory = objectStoreFolder.getRoot().getParentFile();
    when(muleConfiguration.getWorkingDirectory()).thenReturn(workingDirectory.getPath());
    addJavaSerializerToMockMuleContext(muleContext);
    partition = new PersistentObjectStorePartition(muleContext, "test", objectStoreFolder.getRoot());
    partition.open();
  }

  @Test
  public void indicatesUnexistentKeyOnRetrieveError() throws ObjectStoreException {
    final String nonExistentKey = "nonExistentKey";

    try {
      partition.retrieve(nonExistentKey);
      fail("Supposed to thrown an exception as key is not valid");
    } catch (ObjectDoesNotExistException e) {
      assertTrue(e.getMessage().contains(nonExistentKey));
    }
  }

  @Test
  public void skipAndMoveCorruptedOrUnreadableFiles() {
    final String KEY = "key";
    final String VALUE = "value";
    try {
      File.createTempFile("temp", ".obj", objectStoreFolder.getRoot());
      File corruptedFolder = openDirectory(workingDirectory.getAbsolutePath()
          + File.separator + PersistentObjectStorePartition.CORRUPTED_FOLDER);
      int corruptedBefore = corruptedFolder.list().length;

      partition.store(KEY, VALUE);
      // Expect the new stored object, and the partition-descriptor file
      assertEquals(2, objectStoreFolder.getRoot().listFiles().length);

      // Expect to have one more corrupted file in the corrupted folder
      assertEquals(corruptedBefore + 1, corruptedFolder.list().length);
    } catch (Exception e) {
      fail("Supposed to have skipped corrupted or unreadable files");
    }
  }

  @Test
  public void clear() throws Exception {
    final String KEY = "key";
    final String VALUE = "value";


    partition.store(KEY, VALUE);
    assertTrue(partition.contains(KEY));
    assertEquals(VALUE, partition.retrieve(KEY));

    partition.clear();
    assertFalse(partition.contains(KEY));
  }

  @Test
  public void clearBeforeLoading() throws Exception {
    partition.clear();
    assertEquals(0, partition.allKeys().size());
  }
}
