/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.connector;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.api.MessagingException;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionExceptionCode;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnectionProvider;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnector;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreOperations;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStorePoolingProfile;
import org.mule.retry.RetryPolicyExhaustedException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreRetryPolicyConnectionTestCase extends ExtensionFunctionalTestCase
{

    public static final String CONNECTION_FAIL = "Connection fail";
    public static final String CONNECTION_FAIL_DOT = "Connection fail.";

    public PetStoreRetryPolicyConnectionTestCase(){}

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "petstore-retry-policy.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnectorWithConnectionFailure.class};
    }

    @Test
    public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperation() throws Exception
    {
        exception.expect(MessagingException.class);
        exception.expectCause(is(instanceOf(RetryPolicyExhaustedException.class)));
        exception.expectMessage(is(CONNECTION_FAIL_DOT));
        runFlow("fail-operation-with-connection-exception");
    }

    @Test
    public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTime() throws Exception
    {
        exception.expect(MessagingException.class);
        exception.expectCause(is(instanceOf(RetryPolicyExhaustedException.class)));
        exception.expectMessage(is(CONNECTION_FAIL_DOT));
        runFlow("fail-connection-validation");
    }

    @Test
    public void retryPolicyNotExecutedDueToNotConnectionExceptionWithException() throws Exception
    {
        exception.expect(MessagingException.class);
        exception.expectCause(is(instanceOf(Throwable.class)));
        exception.expectMessage(is(CONNECTION_FAIL_DOT));
        runFlow("fail-operation-with-not-handled-exception");
    }

    @Test
    public void retryPolicyNotExecutedDueToNotConnectionExceptionWithThrowable() throws Throwable
    {
        exception.expect(MessagingException.class);
        exception.expectCause(is(instanceOf(Throwable.class)));
        runFlow("fail-operation-with-not-handled-throwable");

    }

    @Extension(name = "petstore", description = "PetStore Test connector")
    @Operations(PetStoreOperationsWithFailures.class)
    @Providers({PooledPetStoreConnectionProviderWithFailureInvalidConnection.class, PooledPetStoreConnectionProviderWithValidConnection.class})
    @Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/petstore", namespace = "petstore", schemaVersion = "4.0")
    public static class PetStoreConnectorWithConnectionFailure extends PetStoreConnector
    {
    }

    @Alias("valid")
    public static class PooledPetStoreConnectionProviderWithValidConnection extends PetStoreConnectionProvider
    {

        @Override
        public ConnectionHandlingStrategy<PetStoreClient> getHandlingStrategy(ConnectionHandlingStrategyFactory handlingStrategyFactory)
        {
            return handlingStrategyFactory.requiresPooling(new PetStorePoolingProfile());
        }
    }

    @Alias("invalid")
    public static class PooledPetStoreConnectionProviderWithFailureInvalidConnection extends PetStoreConnectionProvider
    {
        @Override
        public ConnectionValidationResult validate(PetStoreClient connection)
        {
             return ConnectionValidationResult.failure(CONNECTION_FAIL, ConnectionExceptionCode.INCORRECT_CREDENTIALS, new Exception("Invalid credentials"));
        }

        @Override
        public ConnectionHandlingStrategy<PetStoreClient> getHandlingStrategy(ConnectionHandlingStrategyFactory handlingStrategyFactory)
        {
            return handlingStrategyFactory.requiresPooling(new PetStorePoolingProfile());
        }
    }

    public static class PetStoreOperationsWithFailures extends PetStoreOperations
    {
        @Operation
        public Integer failConnection(@Connection PetStoreClient client) throws ConnectionException
        {
            throw new ConnectionException(CONNECTION_FAIL);
        }

        @Operation
        public Integer failOperationWithException(@Connection PetStoreClient client) throws Exception
        {
            throw new Exception(CONNECTION_FAIL);
        }

        @Operation
        public Integer failOperationWithThrowable(@Connection PetStoreClient client) throws Throwable
        {
            throw new Throwable(CONNECTION_FAIL);
        }
    }


}
