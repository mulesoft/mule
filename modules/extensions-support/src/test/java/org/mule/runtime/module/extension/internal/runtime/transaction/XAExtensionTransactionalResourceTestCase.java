/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static javax.transaction.xa.XAResource.TMSUCCESS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.transaction.XaTransaction;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.transaction.xa.XAResource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class XAExtensionTransactionalResourceTestCase extends AbstractMuleTestCase {

  @Mock
  private XaTransaction transaction;

  @Mock
  private XATransactionalConnection connection;

  @Mock
  private ConnectionHandler connectionHandler;

  @Mock
  private XAResource xaResource;

  private XAExtensionTransactionalResource resource;

  @BeforeEach
  public void before() throws Exception {
    resource = new XAExtensionTransactionalResource(connection, connectionHandler, transaction);
    when(connection.getXAResource()).thenReturn(xaResource);
    when(transaction.enlistResource(xaResource)).thenReturn(true);
    when(transaction.delistResource(xaResource, TMSUCCESS)).thenReturn(true);
    TransactionCoordination.getInstance().bindTransaction(transaction);
  }

  @AfterEach
  public void cleanUp() throws Exception {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  void enlist() throws Exception {
    assertThat(resource.enlist(), is(true));
    verify(transaction).enlistResource(xaResource);
  }

  @Test
  void idempotentEnlist() throws Exception {
    enlist();
    verify(transaction, times(1)).enlistResource(xaResource);
  }

  @Test
  void delist() throws Exception {
    enlist();
    assertThat(resource.delist(), is(true));
    verify(transaction).delistResource(xaResource, TMSUCCESS);
  }

  @Test
  void delistWithoutEnlist() throws Exception {
    assertThat(resource.delist(), is(false));
    verify(transaction, never()).delistResource(same(xaResource), anyInt());
  }

  @Test
  void close() throws Exception {
    resource.close();
    verify(connection, never()).close();
    verify(connectionHandler).release();
  }

  @Test
  void getTargetObject() {
    assertThat(resource.getTargetObject(), is(sameInstance(connection)));
  }
}
