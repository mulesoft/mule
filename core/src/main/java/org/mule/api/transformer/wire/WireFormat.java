/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transformer.wire;

import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;

import java.io.InputStream;
import java.io.OutputStream;


public interface WireFormat extends MuleContextAware
{
    Object read(InputStream is) throws MuleException;

    void write(OutputStream out, Object o, String encoding) throws MuleException;
}
