/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
