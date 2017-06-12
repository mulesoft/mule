/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import com.google.common.hash.HashCode;

import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * Mule {@link javax.transaction.xa.Xid} implementation
 */
public class MuleXid implements Xid, Comparable<Xid> {

  private final int formatId;
  private final byte[] globalTransactionId;
  private final byte[] branchQualifier;

  public MuleXid(int formatId, byte[] globalTransactionId, byte[] branchQualifier) {
    this.formatId = formatId;
    this.globalTransactionId = globalTransactionId;
    this.branchQualifier = branchQualifier;
  }

  public MuleXid(Xid txId) {
    this.formatId = txId.getFormatId();
    this.globalTransactionId = txId.getGlobalTransactionId();
    this.branchQualifier = txId.getBranchQualifier();
  }

  @Override
  public int getFormatId() {
    return formatId;
  }

  @Override
  public byte[] getGlobalTransactionId() {
    return globalTransactionId;
  }

  @Override
  public byte[] getBranchQualifier() {
    return branchQualifier;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Xid)) {
      return false;
    }
    Xid other = (Xid) obj;
    return Arrays.equals(getGlobalTransactionId(), other.getGlobalTransactionId())
        && Arrays.equals(getBranchQualifier(), other.getBranchQualifier()) && getFormatId() == other.getFormatId();
  }

  @Override
  public int hashCode() {
    return formatId * HashCode.fromBytes(globalTransactionId).asInt() * HashCode.fromBytes(branchQualifier).asInt() * 17;
  }

  @Override
  public int compareTo(Xid o) {
    if (formatId == o.getFormatId() && Arrays.equals(globalTransactionId, o.getGlobalTransactionId())
        && Arrays.equals(branchQualifier, o.getBranchQualifier())) {
      return 0;
    }
    return this.hashCode() > o.hashCode() ? 1 : -1;
  }
}
