/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link org.mule.tck.probe.Probe}
 * for cases in which the {@link #isSatisfied()} method can throw
 * exceptions or will perform JUnit/Hamcrest assertions, which result
 * in {@link java.lang.AssertionError}
 * <p/>
 * This implementation automatically takes care of catching those exceptions/errors
 * and returning <code>false</code> in that case
 */
public abstract class JUnitProbe implements Probe
{

    private static final Logger logger = LoggerFactory.getLogger(JUnitProbe.class);

    private AssertionError lastFailure;

    /**
     * Invokes {@link #test()} and returns its outcome, provided that it didn't
     * threw any {@link java.lang.Exception} or {@link java.lang.AssertionError}.
     * Returns <code>false</code> otherwise
     */
    @Override
    public final boolean isSatisfied()
    {
        try
        {
            return test();
        }
        catch (AssertionError e)
        {
            this.lastFailure = e;
            return false;
        }
        catch (Throwable e)
        {
            logger.debug("Probing failed with exception", e);
            return false;
        }
    }

    /**
     * Implement this method to provide your actual probing logic
     *
     * @return <code>true</code> if the acceptance conditions was met. <code>false</code> otherwise
     * @throws Exception
     */
    protected abstract boolean test() throws Exception;

    @Override
    public String describeFailure()
    {
        return lastFailure.getMessage();
    }
}
