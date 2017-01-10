/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.mule.BenchmarkUtils.createMuleContext;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@Fork(1)
@Threads(1)
@BenchmarkMode(AverageTime)
@State(Benchmark)
public class MVELBenchmark
{

    final protected String mel = "StringBuilder sb = new StringBuilder(); fields = payload.split(',\');"
                                 + "if (fields.length > 4) {"
                                 + "    sb.append('  <Contact>\n');"
                                 + "    sb.append('    <FirstName>').append(fields[0]).append('</FirstName>\n');"
                                 + "    sb.append('    <LastName>').append(fields[1]).append('</LastName>\n');"
                                 + "    sb.append('    <Address>').append(fields[2]).append('</Address>\n');"
                                 + "    sb.append('    <TelNum>').append(fields[3]).append('</TelNum>\n');"
                                 + "    sb.append('    <SIN>').append(fields[4]).append('</SIN>\n');"
                                 + "    sb.append('  </Contact>\n');" + "}" + "sb.toString();";

    final protected String payload = "Tom,Fennelly,Male,4,Ireland";

    private MuleContext muleContext;
    private MuleEvent event;

    @Setup
    public void setup() throws MuleException
    {
        muleContext = createMuleContext();
        ((MVELExpressionLanguage) muleContext.getExpressionLanguage()).setAutoResolveVariables(false);
        event = createMuleEvent();
        // Warmup
        for (int i = 0; i < 5000; i++)
        {
            muleContext.getExpressionLanguage().evaluate(mel, event);
        }
    }

    @TearDown
    public void teardown()
    {
        muleContext.dispose();
    }

    /**
     * Cold start: - New expression for each iteration - New context (message) for each iteration
     */
    @Benchmark
    public Object mvelColdStart()
    {
        return muleContext.getExpressionLanguage().evaluate(mel + new Random().nextInt(), createMuleEvent());
    }

    /**
     * Warm start: - Same expression for each iteration - New context (message) for each iteration
     */
    @Benchmark
    public Object mvelWarmStart()
    {
        return muleContext.getExpressionLanguage().evaluate(mel, event);
    }

    /**
     * Hot start: - Same expression for each iteration - Same context (message) for each iteration
     */
    @Benchmark
    public Object mvelHotStart()
    {
        return muleContext.getExpressionLanguage().evaluate(mel, event);
    }

    protected MuleEvent createMuleEvent()
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(payload, muleContext),
                                    MessageExchangePattern.ONE_WAY, (Flow) null);
    }

}
