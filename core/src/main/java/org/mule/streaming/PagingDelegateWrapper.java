/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.util.CollectionUtils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link PagingDelegate} takes care of enforcing some basic
 * behaviour of the delegate contract so that users don't have to. Concerns such as
 * logging, auto closing the delegate if the consumer has been fully consumed, etc
 * are addressed here
 * 
 * @since 3.5.0
 */
public class PagingDelegateWrapper<T> extends PagingDelegate<T>
{

    private static final Logger logger = LoggerFactory.getLogger(PagingDelegateWrapper.class);

    private PagingDelegate<T> wrapped;
    private boolean closed = false;

    public PagingDelegateWrapper(PagingDelegate<T> wrapped)
    {
        this.wrapped = wrapped;
    }

    /**
     * {@inheritDoc} This implementation already takes care of returning
     * <code>null</code> if the delegate is closed or if the obtained page is
     * <code>null</code> or empty. It delegates into the wrapped instance to actually
     * obtain the page.
     */
    public List<T> getPage()
    {
        if (this.closed)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("paging delegate is closed. Returning null");
            }
            return null;
        }

        List<T> page = this.wrapped.getPage();
        if (CollectionUtils.isEmpty(page))
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Empty page was obtained. Closing delegate since this means that the data source has been consumed");
                }

                this.close();
            }
            catch (MuleException e)
            {
                this.handleCloseException(e);
            }
        }

        return page;
    }

    /**
     * {@inheritDoc} Sets the closed flag to true and then delegates into the wrapped
     * instance
     */
    @Override
    public void close() throws MuleException
    {
        this.closed = true;
        this.wrapped.close();
    }

    /**
     * {@inheritDoc} Delegetes into the wrapped instance
     */
    @Override
    public int getTotalResults()
    {
        return this.wrapped.getTotalResults();
    }

    private void handleCloseException(Throwable t)
    {
        if (logger.isWarnEnabled())
        {
            logger.warn("Exception was found trying to close paging delegate. Execution will continue", t);
        }
    }
}
