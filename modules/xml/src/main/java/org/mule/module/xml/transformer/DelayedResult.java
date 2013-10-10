/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.transformer;

import javax.xml.transform.Result;

/**
 * A result type which delays writing until something further down
 * stream can setup the underlying output result.
 */
public interface DelayedResult extends Result
{
    void write(Result result) throws Exception;
}
