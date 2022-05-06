/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.sdk.api.store.ObjectStore;
import org.mule.sdk.api.store.ObjectStoreManager;
import org.mule.sdk.api.store.ObjectStoreSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

public class SdkObjectStoreManagerAdapterTestCase {

  private static final String OS_NAME = "test-os";
  @Rule
  public ExpectedException expectedException = none();
  private org.mule.runtime.api.store.ObjectStoreManager muleObjectStoreManager;
  private org.mule.runtime.api.store.ObjectStore muleObjectStore;
  private SdkObjectStoreManagerAdapter sdkObjectStoreManagerAdapter;

  @Before
  public void setup() {
    muleObjectStoreManager = mock(org.mule.runtime.api.store.ObjectStoreManager.class);
    muleObjectStore = mock(org.mule.runtime.api.store.ObjectStore.class);
    when(muleObjectStoreManager.createObjectStore(eq(OS_NAME), any())).thenReturn(muleObjectStore);
    when(muleObjectStoreManager.getObjectStore(eq(OS_NAME))).thenReturn(muleObjectStore);
    when(muleObjectStoreManager.getOrCreateObjectStore(eq(OS_NAME), any())).thenReturn(muleObjectStore);

    sdkObjectStoreManagerAdapter = new SdkObjectStoreManagerAdapter();
    sdkObjectStoreManagerAdapter.setDelegate(muleObjectStoreManager);
  }

  @Test
  public void adapterIsAnSdkObjectStoreManager() {
    assertThat(sdkObjectStoreManagerAdapter, instanceOf(ObjectStoreManager.class));
  }

  @Test
  public void muleObjectStoreIsCreatedAndAdaptedToSdkObjectStore() {
    ObjectStore sdkObjectStore = sdkObjectStoreManagerAdapter.createObjectStore(OS_NAME, ObjectStoreSettings.builder().build());
    assertThat(sdkObjectStore, instanceOf(SdkObjectStoreAdapter.class));
    verify(muleObjectStoreManager, times(1)).createObjectStore(eq(OS_NAME), any());
  }

  @Test
  public void muleObjectStoreIsRetrievedAndAdaptedToSdkObjectStore() {
    ObjectStore sdkObjectStore = sdkObjectStoreManagerAdapter.getObjectStore(OS_NAME);
    assertThat(sdkObjectStore, instanceOf(SdkObjectStoreAdapter.class));
    verify(muleObjectStoreManager, times(1)).getObjectStore(eq(OS_NAME));
  }

  @Test
  public void muleObjectStoreIsCreatedOrRetrievedAndAdaptedToSdkObjectStore() {
    ObjectStore sdkObjectStore =
        sdkObjectStoreManagerAdapter.getOrCreateObjectStore(OS_NAME, ObjectStoreSettings.builder().build());
    assertThat(sdkObjectStore, instanceOf(SdkObjectStoreAdapter.class));
    verify(muleObjectStoreManager, times(1)).getOrCreateObjectStore(eq(OS_NAME), any());
  }

  @Test
  public void muleObjectStoreIsDisposed() throws Exception {
    sdkObjectStoreManagerAdapter.disposeStore(OS_NAME);
    verify(muleObjectStoreManager, times(1)).disposeStore(eq(OS_NAME));
  }

  @Test
  public void sdkObjectStoreSettingsAreConvertedToMuleObjectStoreSettings() {
    ObjectStoreSettings sdkObjectStoreSettings = ObjectStoreSettings.builder()
        .persistent(true)
        .maxEntries(100)
        .entryTtl(1000L)
        .expirationInterval(5000L)
        .build();
    sdkObjectStoreManagerAdapter.createObjectStore(OS_NAME, sdkObjectStoreSettings);

    ArgumentCaptor<org.mule.runtime.api.store.ObjectStoreSettings> settingsArgumentCaptor = ArgumentCaptor.forClass(
                                                                                                                    org.mule.runtime.api.store.ObjectStoreSettings.class);
    verify(muleObjectStoreManager, times(1)).createObjectStore(eq(OS_NAME), settingsArgumentCaptor.capture());
    org.mule.runtime.api.store.ObjectStoreSettings muleObjectStoreSettings = settingsArgumentCaptor.getValue();

    assertThat(muleObjectStoreSettings.isPersistent(), is(true));
    assertThat(muleObjectStoreSettings.getMaxEntries().get(), is(100));
    assertThat(muleObjectStoreSettings.getEntryTTL().get(), is(1000L));
    assertThat(muleObjectStoreSettings.getExpirationInterval(), is(5000L));
  }

  @Test
  public void defaultSdkObjectStoreSettingsAreConvertedToDefaultMuleObjectStoreSettings() {
    sdkObjectStoreManagerAdapter.createObjectStore(OS_NAME, ObjectStoreSettings.builder().build());

    ArgumentCaptor<org.mule.runtime.api.store.ObjectStoreSettings> settingsArgumentCaptor = ArgumentCaptor.forClass(
                                                                                                                    org.mule.runtime.api.store.ObjectStoreSettings.class);
    verify(muleObjectStoreManager, times(1)).createObjectStore(eq(OS_NAME), settingsArgumentCaptor.capture());
    org.mule.runtime.api.store.ObjectStoreSettings muleObjectStoreSettings = settingsArgumentCaptor.getValue();

    assertThat(muleObjectStoreSettings.isPersistent(), is(true));
    assertThat(muleObjectStoreSettings.getMaxEntries().isPresent(), is(false));
    assertThat(muleObjectStoreSettings.getEntryTTL().isPresent(), is(false));
    assertThat(muleObjectStoreSettings.getExpirationInterval(),
               is(org.mule.runtime.api.store.ObjectStoreSettings.DEFAULT_EXPIRATION_INTERVAL));
  }

  @Test
  public void onFailureMuleObjectStoreExceptionIsThrown() throws Exception {
    doThrow(ObjectStoreException.class).when(muleObjectStoreManager).disposeStore(eq(OS_NAME));
    expectedException.expect(ObjectStoreException.class);
    sdkObjectStoreManagerAdapter.disposeStore(OS_NAME);
  }
}
