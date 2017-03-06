/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import static org.mule.api.util.CredentialsMaskUtil.PASSWORD_PATTERN_NO_QUOTES;
import static org.mule.api.util.CredentialsMaskUtil.USER_PATTERN_NO_QUOTES;
import static org.mule.api.util.CredentialsMaskUtil.maskUrlUserAndPassword;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Creates a {@link Connection} from a {@link DataSource}
 */
public class SimpleConnectionFactory extends AbstractConnectionFactory
{

    @Override
    protected Connection doCreateConnection(DataSource dataSource)
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
        }
        catch (Exception e)
        {
            if (e.getMessage() != null)
            {
                throw new ConnectionCreationException(e, maskUrlUserAndPassword(e.getMessage(), PASSWORD_PATTERN_NO_QUOTES, USER_PATTERN_NO_QUOTES));
            }
            else
            {
                throw new ConnectionCreationException(e);
            }
        }

        return connection;
    }
}
