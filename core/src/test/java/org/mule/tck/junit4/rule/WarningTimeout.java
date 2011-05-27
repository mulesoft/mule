/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.rule;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Defines a {@link MethodRule} that checks for timeouts in the execution
 * of the tests, but differently from the JUnit's {@link org.junit.rules.Timeout}
 * class, just prints a warning in the log and the test still pass.
 * <p>
 * This was implemented in order to maintain the old "failOnTimeout=false"
 * feature from {@link org.mule.tck.junit4.AbstractMuleTestCase}
 */
public class WarningTimeout implements MethodRule
{

    private final int milliseconds;

    public WarningTimeout(int milliseconds)
    {
        this.milliseconds = milliseconds;
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target)
    {
        return new WarnOnTimeout(base, milliseconds);
    }
}
