/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import static org.mule.api.metadata.DataType.STRING_DATA_TYPE;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.execution.CompletionHandler;
import org.mule.api.temp.MuleMessage;
import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.runtime.source.Source;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@Alias("PushMeth")
public class HeisenbergSource extends Source<Void, Serializable>
{
    private ScheduledExecutorService executor;

    @UseConfig
    private HeisenbergExtension heisenberg;

    @Connection
    private HeisenbergConnection connection;

    @Parameter
    private volatile int initialBatchNumber;

    @Inject
    private MuleContext muleContext;

    @Override
    public void start()
    {
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> sourceContext.getMessageHandler().handle(makeMessage(), new CompletionHandler<MuleMessage<Void, Serializable>, Exception>()
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
        }), 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop()
    {
        executor.shutdownNow();
        try
        {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private MuleMessage makeMessage()
    {
        String payload = String.format("Meth Batch %d. If found by DEA contact %s", ++initialBatchNumber, connection.getSaulPhoneNumber());
        return new DefaultMuleMessage(payload, null, STRING_DATA_TYPE, muleContext);
    }


}
