/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.param.UseConfig;

import java.math.BigDecimal;

public class MoneyLaunderingOperation
{

    private long totalLaunderedAmount = 0;

    @Operation
    public synchronized Long launder(@UseConfig HeisenbergExtension config, long amount)
    {
        config.setMoney(config.getMoney().subtract(BigDecimal.valueOf(amount)));
        totalLaunderedAmount += amount;
        return totalLaunderedAmount;
    }


}
