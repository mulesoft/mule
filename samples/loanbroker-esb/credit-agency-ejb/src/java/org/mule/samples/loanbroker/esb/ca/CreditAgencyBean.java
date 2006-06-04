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
package org.mule.samples.loanbroker.esb.ca;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import java.text.MessageFormat;

/**
 * <code>CreditAgencyBean</code> obtains a credit historey record for a
 * customer.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CreditAgencyBean implements SessionBean {
    private SessionContext ctx;

    private static final String MSG="<credit-profile><customer-name>{0}</customer-name><customer-ssn>{1}</customer-ssn><credit-score>{2}</credit-score><customer-history>{3}</customer-history></credit-profile>";

    public void ejbActivate() throws EJBException {
    }

    public void ejbPassivate() throws EJBException {
    }

    public void ejbRemove() throws EJBException {
    }

    public void ejbCreate() throws EJBException {
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException {
        ctx = sessionContext;
    }

    protected int getCreditScore(int ssn) {
        int credit_score;

        credit_score = (int) (Math.random() * 600 + 300);

        return credit_score;
    }

    protected int getCreditHistoryLength(int ssn) {
        int credit_history_length;

        credit_history_length = (int) (Math.random() * 19 + 1);

        return credit_history_length;
    }


    /**
     * Used by Ejb Call
     * @param name
     * @param ssn
     * @return
     */
    public String getCreditProfile(String name, Integer ssn)
    {
        String msg = MessageFormat.format(MSG, new Object[]{name, ssn,
                       new Integer(getCreditScore(ssn.intValue())),
                       new Integer(getCreditHistoryLength(ssn.intValue()))});
        return msg;
    }

}
