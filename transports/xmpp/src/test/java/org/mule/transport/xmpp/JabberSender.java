/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

public class JabberSender extends RunnableWithExceptionHandler
{
    public interface Callback
    {
        void doit() throws Exception;
    }

    private Callback callback;
    
    public JabberSender(Callback callback)
    {
        super();
        this.callback = callback;
    }
    
    @Override
    protected void doRun() throws Exception
    {
        callback.doit();
    }
}


