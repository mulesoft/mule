/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.junit4.rule;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Defines a {@link TestRule} that checks for timeouts in the execution
 * of the tests, but differently from the JUnit's {@link org.junit.rules.Timeout}
 * class, just prints a warning in the log and the test still pass.
 * <p>
 * This was implemented in order to maintain the old "failOnTimeout=false"
 * feature from {@link org.mule.tck.junit4.AbstractMuleTestCase}
 */
public class WarningTimeout implements TestRule
{

    private final int milliseconds;

    public WarningTimeout(int milliseconds)
    {
        this.milliseconds = milliseconds;
    }

    public Statement apply(Statement statement, Description description)
    {
        return new WarnOnTimeout(statement, milliseconds);
    }
}
