/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb.message;

import java.io.Serializable;

/**
 * <code>CreditProfile</code> is a dummy finance profile for a customer
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CreditProfile implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5924690191061177417L;

    private int creditScore;
    private int creditHistory;

    public int getCreditScore()
    {
        return creditScore;
    }

    public void setCreditScore(int creditScore)
    {
        this.creditScore = creditScore;
    }

    public int getCreditHistory()
    {
        return creditHistory;
    }

    public void setCreditHistory(int creditHistory)
    {
        this.creditHistory = creditHistory;
    }


}
