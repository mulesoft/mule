/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.mule.sdk.api.store.ObjectAlreadyExistsException;
import org.mule.sdk.api.store.ObjectDoesNotExistException;
import org.mule.sdk.api.store.ObjectStore;
import org.mule.sdk.api.store.ObjectStoreException;
import org.mule.sdk.api.store.ObjectStoreNotAvailableException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SdkObjectStoreAdapterTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private org.mule.runtime.api.store.ObjectStore muleObjectStore;

  @Before
  public void setup() {
    muleObjectStore = mock(org.mule.runtime.api.store.ObjectStore.class);
  }

  @Test
  public void nullValueCannotBeAdaptedAndThrowsException() {
    expectedException.expect(IllegalArgumentException.class);
    SdkObjectStoreAdapter.from(null);
  }

  @Test
  public void muleObjectStoreIsAdaptedToSdkObjectStore() {
    assertThat(SdkObjectStoreAdapter.from(muleObjectStore), instanceOf(ObjectStore.class));
  }

  @Test
  public void sdkObjectStoreIsNotAdapted() {
    ObjectStore objectStore = SdkObjectStoreAdapter.from(muleObjectStore);
    assertThat(SdkObjectStoreAdapter.from(objectStore), sameInstance(objectStore));
  }

  @Test
  public void muleObjectStoreExceptionIsConvertedToSdkObjectStoreException() throws Exception {
    doThrow(org.mule.runtime.api.store.ObjectStoreException.class).when(muleObjectStore).clear();
    expectedException.expect(ObjectStoreException.class);
    SdkObjectStoreAdapter.from(muleObjectStore).clear();
  }

  @Test
  public void muleObjectAlreadyExistsExceptionIsConvertedToSdkObjectAlreadyExistsException() throws Exception {
    doThrow(org.mule.runtime.api.store.ObjectAlreadyExistsException.class).when(muleObjectStore).store("key", "value");
    expectedException.expect(ObjectAlreadyExistsException.class);
    SdkObjectStoreAdapter.from(muleObjectStore).store("key", "value");
  }

  @Test
  public void muleObjectDoesNotExistExceptionIsConvertedToSdkObjectDoesNotExistException() throws Exception {
    doThrow(org.mule.runtime.api.store.ObjectDoesNotExistException.class).when(muleObjectStore).remove("key");
    expectedException.expect(ObjectDoesNotExistException.class);
    SdkObjectStoreAdapter.from(muleObjectStore).remove("key");
  }

  @Test
  public void muleObjectStoreNotAvailableExceptionIsConvertedToSdkObjectStoreNotAvailableException() throws Exception {
    doThrow(org.mule.runtime.api.store.ObjectStoreNotAvailableException.class).when(muleObjectStore).contains("key");
    expectedException.expect(ObjectStoreNotAvailableException.class);
    SdkObjectStoreAdapter.from(muleObjectStore).contains("key");
  }
}
