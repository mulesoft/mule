/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis;

/**
 * TradeStatus.java
 *
 * This file was auto-generated from WSDL
 * by the IBM Web services WSDL2Java emitter.
 * jdk0450.04 v122904173847
 */

public class TradeStatus  implements java.io.Serializable {
    private static final long serialVersionUID = 414517174955602939L;

    private java.lang.String status;
    private int tradeID;

    public TradeStatus()
    {
        super();
    }

    public java.lang.String getStatus() {
        return status;
    }

    public void setStatus(java.lang.String status) {
        this.status = status;
    }

    public int getTradeID() {
        return tradeID;
    }

    public void setTradeID(int tradeID) {
        this.tradeID = tradeID;
    }

    private transient java.lang.ThreadLocal __history;
    public boolean equals(java.lang.Object obj) {
        if (obj == null) { return false; }
        if (obj.getClass() != this.getClass()) { return false;}
        TradeStatus other = (TradeStatus) obj;
        boolean _equals;
        _equals = true
            && ((this.status==null && other.getStatus()==null) ||
             (this.status!=null &&
              this.status.equals(other.getStatus())))
            && this.tradeID == other.getTradeID();
        if (!_equals) { return false; }
        if (__history == null) {
            synchronized (this) {
                if (__history == null) {
                    __history = new java.lang.ThreadLocal();
                }
            }
        }
        TradeStatus history = (TradeStatus) __history.get();
        if (history != null) { return (history == obj); }
        if (this == obj) {
            return true;
        }
        __history.set(obj);
        __history.set(null);
        return true;
    }

    private transient java.lang.ThreadLocal __hashHistory;
    public int hashCode() {
        if (__hashHistory == null) {
            synchronized (this) {
                if (__hashHistory == null) {
                    __hashHistory = new java.lang.ThreadLocal();
                }
            }
        }
        TradeStatus history = (TradeStatus) __hashHistory.get();
        if (history != null) { return 0; }
        __hashHistory.set(this);
        int _hashCode = 1;
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        _hashCode += getTradeID();
        __hashHistory.set(null);
        return _hashCode;
    }

}
