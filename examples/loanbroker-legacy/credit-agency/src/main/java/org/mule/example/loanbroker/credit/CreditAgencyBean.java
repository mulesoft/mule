/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.credit;

import java.text.MessageFormat;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * <code>CreditAgencyBean</code> obtains a credit historey record for a customer.
 */
public class CreditAgencyBean implements SessionBean
{
    private static final long serialVersionUID = 1546168214387311746L;

    private static final String MSG = "<credit-profile><customer-name>{0}</customer-name><customer-ssn>{1}</customer-ssn><credit-score>{2}</credit-score><customer-history>{3}</customer-history></credit-profile>";

    public void ejbActivate() throws EJBException
    {
        // nothing to do
    }

    public void ejbPassivate() throws EJBException
    {
        // nothing to do
    }

    public void ejbRemove() throws EJBException
    {
        // nothing to do
    }

    public void ejbCreate() throws EJBException
    {
        // nothing to do
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException
    {
        // SessionContext can be ignored
    }

    protected int getCreditScore(int ssn)
    {
        int credit_score;

        credit_score = (int)(Math.random() * 600 + 300);

        return credit_score;
    }

    protected int getCreditHistoryLength(int ssn)
    {
        int credit_history_length;

        credit_history_length = (int)(Math.random() * 19 + 1);

        return credit_history_length;
    }

    /**
     * Used by Ejb Call
     */
    public String getCreditProfile(String name, Integer ssn)
    {
        String msg = MessageFormat.format(MSG, name, ssn,
                                          getCreditScore(ssn), getCreditHistoryLength(ssn));
        return msg;
    }

}
