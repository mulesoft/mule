/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.polling.watermark.selector.MaxValueWatermarkSelector;
import org.mule.transport.polling.watermark.selector.MinValueWatermarkSelector;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Test;

@SmallTest
public class WatermarkSelectorTestCase extends AbstractMuleTestCase
{
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private Calendar calendar = Calendar.getInstance();

    @Test
    public void testMaxSelected()
    {
        MaxValueWatermarkSelector selector = new MaxValueWatermarkSelector();

        String max = null;

        for (int i = 0; i < 19; i++)
        {
            calendar.add(Calendar.DAY_OF_YEAR, 100);
            max = format.format(calendar.getTime());
            selector.acceptValue(max);
        }

        assertThat(max, equalTo(selector.getSelectedValue()));
    }

    @Test
    public void testMinSelected()
    {
        MinValueWatermarkSelector selector = new MinValueWatermarkSelector();

        String min = null;
        for (int i = 0; i < 10; i++)
        {
            calendar.add(Calendar.DAY_OF_YEAR, 100);
            if (min == null)
            {
                min = format.format(calendar.getTime());
            }

            selector.acceptValue(min);
        }

        assertThat(min, equalTo(selector.getSelectedValue()));
    }

}
