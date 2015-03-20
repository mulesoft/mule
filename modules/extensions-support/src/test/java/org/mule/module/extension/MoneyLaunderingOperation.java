/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.annotations.ImplementationOf;
import org.mule.extension.annotations.Operation;

import java.math.BigDecimal;

public class MoneyLaunderingOperation
{

    private final HeisenbergExtension config;

    private long totalLaunderedAmount = 0;

    public MoneyLaunderingOperation(HeisenbergExtension config)
    {
        this.config = config;
    }

    @Operation
    @ImplementationOf(HeisenbergExtension.class)
    public synchronized Long launder(long amount)
    {
        config.setMoney(config.getMoney().subtract(BigDecimal.valueOf(amount)));
        totalLaunderedAmount += amount;
        return totalLaunderedAmount;
    }


}
