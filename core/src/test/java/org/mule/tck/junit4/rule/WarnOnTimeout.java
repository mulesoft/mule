/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.junit4.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.Statement;

/**
 * Defines a {@link Statement} to execute a test with a given timeout.
 * Differently from JUnit's {@link org.junit.internal.runners.statements.FailOnTimeout}
 * this statement just prints a warning in the log, so the test will pass in
 * case of timeout.
 */
public class WarnOnTimeout extends Statement
{

    private final Log logger = LogFactory.getLog(this.getClass());

    private Statement next;
    private final long timeout;
    private boolean finished = false;
    private Throwable thrown = null;

    public WarnOnTimeout(Statement next, long timeout)
    {
        this.next = next;
        this.timeout = timeout;
    }

    @Override
    public void evaluate() throws Throwable
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    next.evaluate();
                    finished = true;
                }
                catch (Throwable e)
                {
                    thrown = e;
                }
            }
        };
        thread.start();
        thread.join(timeout);
        if (finished)
        {
            return;
        }
        if (thrown != null)
        {
            throw thrown;
        }

        logger.warn("Timeout of " + timeout + "ms exceeded");
    }
}
