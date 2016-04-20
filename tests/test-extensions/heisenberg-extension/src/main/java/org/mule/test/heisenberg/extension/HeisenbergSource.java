/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.metadata.DataType.STRING_DATA_TYPE;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


@Alias("ListenPayments")
public class HeisenbergSource extends Source<Void, Serializable>
{
    public static final String CORE_POOL_SIZE_ERROR_MESSAGE = "corePoolSize cannot be a negative value";
    public static final String INITIAL_BATCH_NUMBER_ERROR_MESSAGE = "initialBatchNumber cannot be a negative value";

    private ScheduledExecutorService executor;

    @UseConfig
    private HeisenbergExtension heisenberg;

    @Connection
    private HeisenbergConnection connection;

    @Parameter
    private volatile int initialBatchNumber;

    @Parameter
    @Optional(defaultValue = "1")
    private int corePoolSize;

    @Inject
    private MuleContext muleContext;

    @Override
    public void start()
    {
        HeisenbergExtension.sourceTimesStarted++;

        if (corePoolSize < 0)
        {
            throw new RuntimeException(CORE_POOL_SIZE_ERROR_MESSAGE);
        }

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> sourceContext.getMessageHandler().handle(makeMessage(sourceContext), completionHandler()), 0, 100, TimeUnit.MILLISECONDS);
    }

    private CompletionHandler<MuleMessage<Void, Serializable>, Exception> completionHandler()
    {
        return new CompletionHandler<MuleMessage<Void, Serializable>, Exception>()
        {
            @Override
            public void onCompletion(MuleMessage message)
            {
                Long payment = (Long) message.getPayload();
                heisenberg.setMoney(heisenberg.getMoney().add(BigDecimal.valueOf(payment)));
            }

            @Override
            public void onFailure(Exception exception)
            {
                heisenberg.setMoney(BigDecimal.valueOf(-1));
            }
        };
    }

    @Override
    public void stop()
    {
        if (executor != null)
        {
            executor.shutdown();
            try
            {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private MuleMessage makeMessage(SourceContext sourceContext)
    {
        if (initialBatchNumber < 0)
        {
            sourceContext.getExceptionCallback().onException(new RuntimeException(INITIAL_BATCH_NUMBER_ERROR_MESSAGE));
        }

        String payload = String.format("Meth Batch %d. If found by DEA contact %s", ++initialBatchNumber, connection.getSaulPhoneNumber());
        return new DefaultMuleMessage(payload, null, STRING_DATA_TYPE, muleContext);
    }


}
