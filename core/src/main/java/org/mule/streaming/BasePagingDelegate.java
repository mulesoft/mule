/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.api.streaming.PagingDelegate;
import org.mule.util.CollectionUtils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link PagingDelegate} that takes care of some basic
 * concerns such as logging, auto closing the delegate if the consumer has been fully
 * consumed, etc.
 */
public abstract class BasePagingDelegate<T> implements PagingDelegate<T>
{

    private static final Logger logger = LoggerFactory.getLogger(BasePagingDelegate.class);

    private boolean closed = false;

    /**
     * {@inheritDoc} This base implementation already takes care of returning
     * <code>null</code> if the delegate is closed or if the obtained page is
     * <code>null</code> or empty. It delegates into {@link
     * org.mule.streaming.BasePagingDelegate.doGetPage()} to actually obtain the
     * page.
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

        List<T> page = this.doGetPage();
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
     * Implement this method to actually fetch the page
     */
    protected abstract List<T> doGetPage();

    /**
     * Sets the closed flag to true and then delegates into {@link
     * org.mule.streaming.BasePagingDelegate.doClose()}
     */
    @Override
    public void close() throws MuleException
    {
        this.closed = true;
        this.doClose();
    }

    /**
     * Implement this to actually release any pending resources
     * 
     * @throws MuleException
     */
    protected abstract void doClose() throws MuleException;

    private void handleCloseException(Throwable t)
    {
        if (logger.isWarnEnabled())
        {
            logger.warn("Exception was found trying to close paging delegate. Execution will continue", t);
        }
    }
}
