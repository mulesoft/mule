/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.apache.maven.enforcer.rule;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;

public class EnforcerPortRule implements EnforcerRule
{
    /**
     * Simple param. This rule will fail if the value is true.
     */
    private boolean shouldIfail = false;
    private int port = 0;

    /**
     * Check port availability. Taken from:
     * http://svn.apache.org/viewvc/mina/trunk/core
     * /src/main/java/org/apache/mina/util/AvailablePortFinder.java?view=markup
     */
    public static boolean available(int port)
    {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try
        {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        }
        catch (IOException e)
        {
        }
        finally
        {
            if (ds != null)
            {
                ds.close();
            }

            if (ss != null)
            {
                try
                {
                    ss.close();
                }
                catch (IOException e)
                {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException
    {
        Log log = helper.getLog();
        log.info("checking availability of port : " + this.port);

        // make sure it's > 0
        if (!(this.port > 0))
        {
            throw new EnforcerRuleException("Port is not valid " + this.port);
        }

        // check availability
        if (!available(this.port))
        {
            throw new EnforcerRuleException("Port is not available " + this.port);
        }

        if (this.shouldIfail)
        {
            throw new EnforcerRuleException("Failing because my param said so.");
        }
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or
     * conditions change that would cause the result to be different. Multiple cached
     * results are stored based on their id. The easiest way to do this is to return
     * a hash computed from the values of your parameters. If your rule is not
     * cacheable, then the result here is not important, you may return anything.
     */
    public String getCacheId()
    {
        // no hash on boolean...only parameter so no hash is needed.
        return "" + this.shouldIfail;
    }

    /**
     * This tells the system if the results are cacheable at all. Keep in mind that
     * during forked builds and other things, a given rule may be executed more than
     * once for the same project. This means that even things that change from
     * project to project may still be cacheable in certain instances.
     */
    public boolean isCacheable()
    {
        return false;
    }

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored
     * results are passed to this method to allow double checking of the results.
     * Most of the time this can be done by generating unique ids, but sometimes the
     * results of objects returned by the helper need to be queried. You may for
     * example, store certain objects in your rule and then query them later.
     */
    public boolean isResultValid(EnforcerRule arg0)
    {
        return false;
    }
}
