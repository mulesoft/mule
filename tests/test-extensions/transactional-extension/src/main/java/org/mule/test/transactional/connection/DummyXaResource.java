/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueSession;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyXaResource implements XAResource, QueueSession {

  private boolean commitStarted;
  private boolean commitEnded;
  private boolean rollbackExecuted;
  private boolean isTxStarted;
  private boolean prepared;

  protected static final Logger logger = LoggerFactory.getLogger(DummyXaResource.class);

  @Override
  public void commit(Xid xid, boolean b) throws XAException {
    this.commitStarted = true;
    logger.debug("Committing XA TX. {}, One Face: {}", xid, b);
  }

  @Override
  public void end(Xid xid, int i) throws XAException {
    this.commitEnded = true;
    logger.debug("Committing XA TX. {}, Flags: {}", xid, i);
  }

  @Override
  public void forget(Xid xid) throws XAException {
    logger.debug("Forgetting XA TX. {}", xid);
  }

  @Override
  public int getTransactionTimeout() throws XAException {
    return 0;
  }

  @Override
  public boolean isSameRM(XAResource xaResource) throws XAException {
    return true;
  }

  @Override
  public int prepare(Xid xid) throws XAException {
    logger.debug("Preparing XA TX. {}", xid);
    this.prepared = true;
    return 0;
  }

  @Override
  public Xid[] recover(int i) throws XAException {
    logger.debug("Recovering XA TX. {}", i);
    return new Xid[0];
  }

  @Override
  public void rollback(Xid xid) throws XAException {
    this.rollbackExecuted = true;
    logger.debug("Recovering XA TX. {}", xid);
  }

  @Override
  public boolean setTransactionTimeout(int i) throws XAException {
    logger.debug("Setting XA TX Timeout. {}", i);
    return true;
  }

  @Override
  public void start(Xid xid, int i) throws XAException {
    logger.debug("Started XA TX. {} {}", xid, i);
    this.isTxStarted = true;
  }

  @Override
  public Queue getQueue(String name) {
    return null;
  }

  @Override
  public void begin() throws ResourceManagerException {

  }

  @Override
  public void commit() throws ResourceManagerException {

  }

  @Override
  public void rollback() throws ResourceManagerException {

  }

  public boolean isCommitStarted() {
    return commitStarted;
  }

  public boolean isPrepared() {
    return prepared;
  }

  public boolean isTxEnded() {
    return commitEnded;
  }

  public boolean isRollbackExecuted() {
    return rollbackExecuted;
  }

  public boolean isTxStarted() {
    return isTxStarted;
  }

  public static Matcher<DummyXaResource> commitStarted(boolean isCommitStarted) {
    return new TypeSafeMatcher<DummyXaResource>() {

      @Override
      protected boolean matchesSafely(DummyXaResource item) {
        return item.isCommitStarted() == isCommitStarted;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("isCommitStarted == " + isCommitStarted);
      }
    };

  }

  public static Matcher<DummyXaResource> prepared(boolean isPrepared) {
    return new TypeSafeMatcher<DummyXaResource>() {

      @Override
      protected boolean matchesSafely(DummyXaResource item) {
        return item.isPrepared() == isPrepared;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("isPrepared == " + isPrepared);
      }
    };

  }

  public static Matcher<DummyXaResource> txEnded() {
    return new TypeSafeMatcher<DummyXaResource>() {

      @Override
      protected boolean matchesSafely(DummyXaResource item) {
        return item.isTxEnded();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("isTxEnded");
      }
    };

  }

  public static Matcher<DummyXaResource> rollbackExecuted(boolean isRollback) {
    return new TypeSafeMatcher<DummyXaResource>() {

      @Override
      protected boolean matchesSafely(DummyXaResource item) {
        return item.isRollbackExecuted() == isRollback;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("isRollbackExecuted == " + isRollback);
      }
    };

  }

  public static Matcher<DummyXaResource> txStarted() {
    return new TypeSafeMatcher<DummyXaResource>() {

      @Override
      protected boolean matchesSafely(DummyXaResource item) {
        return item.isTxStarted();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("isTxStarted");
      }
    };

  }
}
