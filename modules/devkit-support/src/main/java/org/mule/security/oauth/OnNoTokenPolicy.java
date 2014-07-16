/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.MetadataAware;
import org.mule.api.MuleEvent;
import org.mule.common.security.oauth.exception.NotAuthorizedException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This enum specifies the behavior to take when an OAuth protected operation is
 * executed before the authorization is performed
 */
public enum OnNoTokenPolicy
{

    /**
     * This policy throws a
     * {@link org.mule.common.security.oauth.exception.NotAuthorizedException}
     */
    EXCEPTION
    {
        @Override
        public MuleEvent handleNotAuthorized(Object source, NotAuthorizedException e, MuleEvent event)
            throws NotAuthorizedException
        {
            throw e;
        }
    },

    /**
     * This policy returns null which means that the processor chain should stop and
     * the rest of the processors should not be executed. It behaves similarly to a
     * message filter
     */
    STOP_FLOW
    {
        @Override
        public MuleEvent handleNotAuthorized(Object source, NotAuthorizedException e, MuleEvent event)
            throws NotAuthorizedException
        {
            if (logger.isWarnEnabled())
            {
                StringBuilder builder = new StringBuilder();
                builder.append("Tried to execute OAuth protected operation but the connector was not authorized yet. Stopping execution per OnNoTokenPolicy configuration.");

                if (!StringUtils.isBlank(e.getAccessTokenId()))
                {
                    builder.append(" [accessTokenId = ").append(e.getAccessTokenId()).append("]");
                }

                if (source instanceof MetadataAware)
                {
                    MetadataAware metadata = (MetadataAware) source;
                    builder.append(" [connector= ").append(metadata.getModuleName());
                }
                else
                {
                    builder.append(" [source class= ").append(source.getClass().getCanonicalName());
                }

                logger.warn(builder.toString());
            }

            // returning a null event causes the chain to stop processing
            return null;

        };
    };

    /**
     * This method handles a
     * {@link org.mule.common.security.oauth.exception.NotAuthorizedException}
     * according to each policy
     * 
     * @param source the unauthorized connector
     * @param e the exception thrown
     * @param event the current mule event
     * @return the same event that was received or a new/modified one. It could also
     *         return <code>null</code> to signal that the chain execution should
     *         stop
     * @throws NotAuthorizedException if the policy decides to simply bubble up the
     *             exception
     */
    public abstract MuleEvent handleNotAuthorized(Object source,
                                                  NotAuthorizedException e,
                                                  MuleEvent event) throws NotAuthorizedException;

    private static final Logger logger = LoggerFactory.getLogger(OnNoTokenPolicy.class);

}
