/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.math.BigDecimal;

public class MoneyLaunderingOperation
{

    private long totalLaunderedAmount = 0;

    public synchronized Long launder(@UseConfig HeisenbergExtension config, long amount)
    {
        config.setMoney(config.getMoney().subtract(BigDecimal.valueOf(amount)));
        totalLaunderedAmount += amount;
        return totalLaunderedAmount;
    }


}
