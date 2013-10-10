/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.source;

import org.mule.api.MuleException;

import java.util.List;

/**
 * Composes multiple {@link MessageSource}s.
 */
public interface CompositeMessageSource extends MessageSource
{
    void addSource(MessageSource messageSource) throws MuleException;

    void removeSource(MessageSource messageSource) throws MuleException;

    List<MessageSource> getSources();

}
