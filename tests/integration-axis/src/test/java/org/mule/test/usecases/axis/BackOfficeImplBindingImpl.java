/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis;

public class BackOfficeImplBindingImpl implements org.mule.test.usecases.axis.BackOfficeImpl
{
    // Doc Lit test
    public SubmitTradeResponse submitTrade(SubmitTrade parameters)
    {
        TradeStatus ts = new TradeStatus();
        Trade trade = parameters.getArg0();
        ts.setTradeID(trade.getTradeID());
        ts.setStatus("RECEIVED");
        SubmitTradeResponse str = new SubmitTradeResponse(ts);
        return str;
    }

    // RPC Enc test
    public TradeStatus submitTrade(Trade trade)
    {
        TradeStatus ts = new TradeStatus();
        ts.setTradeID(trade.getTradeID());
        ts.setStatus("RECEIVED");
        return ts;
    }

    // Wrapped Lit test
    public TradeStatus submitTrade(int accountID, String cusip, int currency, int tradeID, int transaction)
    {
        Trade trade = new Trade();
        trade.setAccountID(accountID);
        trade.setCusip(cusip);
        trade.setCurrency(currency);
        trade.setTradeID(tradeID);
        trade.setTransaction(transaction);

        TradeStatus ts = new TradeStatus();
        ts.setTradeID(trade.getTradeID());
        ts.setStatus("RECEIVED");
        return ts;
    }

}
