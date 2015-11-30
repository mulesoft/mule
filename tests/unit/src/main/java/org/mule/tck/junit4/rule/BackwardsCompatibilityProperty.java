/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import org.mule.util.BackwardsCompatibilityPropertyChecker;

import org.junit.rules.ExternalResource;

/**
 * Sets up the override feature of a BackwardsCompatibilityPropertyChecker allowing for it to be
 * enabled and guarantying that it is removed afterwards.
 */
public class BackwardsCompatibilityProperty extends ExternalResource
{
    private final BackwardsCompatibilityPropertyChecker propertyChecker;

    public BackwardsCompatibilityProperty(BackwardsCompatibilityPropertyChecker propertyChecker)
    {
        this.propertyChecker = propertyChecker;
    }

    public void switchOn()
    {
        propertyChecker.setOverride(true);
    }

    @Override
    protected void before() throws Throwable
    {
        propertyChecker.setOverride(false);
    }

    @Override
    protected void after()
    {
        propertyChecker.removeOverride();
    }

}
