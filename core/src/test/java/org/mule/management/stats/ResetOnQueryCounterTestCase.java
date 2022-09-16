/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.Transformer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@SmallTest
@RunWith(Parameterized.class)
public class ResetOnQueryCounterTestCase extends AbstractMuleContextTestCase
{
  
    @Parameters(name = "{0}")
    public static List<Object[]> params() 
    {
        return asList(
                      new Object[]
                      {
                          "eventsReceivedSync",
                          new Transformer()
                          {
                              @Override
                              public Object transform(Object statistics)
                              {
                                  return ((FlowConstructStatistics)statistics).getEventsReceivedCounter();
                              }
                          },
                          new Closure()
                          {
                              @Override
                              public void execute(Object statistics)
                              {
                                  ((FlowConstructStatistics) statistics).incReceivedEventSync();
                              }
                          }
                      },
                      new Object[]
                          {
                              "eventsReceivedASync",
                              new Transformer()
                              {
                                @Override
                                public Object transform(Object statistics)
                                {
                                  return ((FlowConstructStatistics)statistics).getEventsReceivedCounter();
                                }
                              },
                              new Closure()
                              {
                                @Override
                                public void execute(Object statistics)
                                {
                                  ((FlowConstructStatistics) statistics).incReceivedEventASync();
                                }
                              }
                          },
                      new Object[]
                      {
                          "executionErrors",
                          new Transformer()
                          {
                              @Override
                              public Object transform(Object statistics)
                              {
                                  return ((FlowConstructStatistics)statistics).getExecutionErrorsCounter();
                              }
                          },
                          new Closure()
                          {
                              @Override
                              public void execute(Object statistics)
                              {
                                  ((FlowConstructStatistics) statistics).incExecutionError();
                              }
                          }
                      },
                      new Object[]
                      {
                          "connectionErrors",
                          new Transformer()
                          {
                              @Override
                              public Object transform(Object statistics)
                              {
                                  return ((FlowConstructStatistics)statistics).getConnectionErrorsCounter();
                              }
                          },
                          new Closure()
                          {
                              @Override
                              public void execute(Object statistics)
                              {
                                  ((FlowConstructStatistics) statistics).incConnectionErrors();
                              }
                          }
                      },
                      new Object[]
                      {
                          "fatalErrors",
                          new Transformer()
                          {
                              @Override
                              public Object transform(Object statistics)
                              {
                                  return ((FlowConstructStatistics)statistics).getFatalErrorsCounter();
                              }
                          },
                          new Closure()
                          {
                              @Override
                              public void execute(Object statistics)
                              {
                                  ((FlowConstructStatistics) statistics).incFatalError();
                              }
                          }
                      });
    }
  
    @Parameter(0)
    public String paramsConfigName;
  
    @Parameter(1)
    public Transformer createCounter;
  
    @Parameter(2)
    public Closure incrementCounter;
  
    private FlowConstructStatistics flow1Stats;
    private FlowConstructStatistics flow2Stats;
    private AllStatistics allStatistics;
  
    @Before
    public void before()
    {
        flow1Stats = new FlowConstructStatistics("Flow", "someFlow1");
        flow2Stats = new FlowConstructStatistics("Flow", "someFlow2");
    
        allStatistics = new AllStatistics();
        allStatistics.add(flow1Stats);
        allStatistics.add(flow2Stats);
    }
  
    @Test
    public void flowCountersInitialState()
    {
        ResetOnQueryCounter beforeIncCounter = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
        incrementCounter.execute(flow1Stats);
        ResetOnQueryCounter afterIncCounter = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
    
        assertThat(beforeIncCounter.getAndReset(), is(1L));
        assertThat(afterIncCounter.getAndReset(), is(1L));
    }
  
    @Test
    public void flowCountersIndependentFromEachOther()
    {
        ResetOnQueryCounter counterA = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
        ResetOnQueryCounter counterB = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
        incrementCounter.execute(flow1Stats);
    
        assertThat(counterA.getAndReset(), is(1L));
        assertThat(counterA.get(), is(0L));
        assertThat(counterB.get(), is(1L));
    }
  
    @Test
    public void flowCountersUnaffectdByClear()
    {
        ResetOnQueryCounter counter = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
    
        incrementCounter.execute(flow1Stats);
        assertThat(counter.get(), is(1L));
    
        flow1Stats.clear();
        assertThat(counter.get(), is(1L));
    }
  
    @Test
    public void appLevelAggregationCountersInitialState()
    {
        ResetOnQueryCounter beforeIncEventsReceivedCounter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
        incrementCounter.execute(flow1Stats);
        ResetOnQueryCounter after1IncEventsReceivedCounter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
        incrementCounter.execute(flow2Stats);
        ResetOnQueryCounter after2IncEventsReceivedCounter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
    
        assertThat(beforeIncEventsReceivedCounter.getAndReset(), is(2L));
        assertThat(after1IncEventsReceivedCounter.getAndReset(), is(2L));
        assertThat(after2IncEventsReceivedCounter.getAndReset(), is(2L));
    }
  
    @Test
    public void appLevelAggregationCountersIndependentFromEachOther()
    {
        ResetOnQueryCounter counterA = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
        ResetOnQueryCounter counterB = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
    
        incrementCounter.execute(flow1Stats);
    
        assertThat(counterA.getAndReset(), is(1L));
        assertThat(counterA.get(), is(0L));
        assertThat(counterB.get(), is(1L));
    
        incrementCounter.execute(flow2Stats);
    
        assertThat(counterA.getAndReset(), is(1L));
        assertThat(counterA.get(), is(0L));
        assertThat(counterB.get(), is(2L));
    }
  
    @Test
    public void appLevelAggregationAndFlowCountersIndependentFromEachOther()
    {
        ResetOnQueryCounter flow1Counter = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
        ResetOnQueryCounter counter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
    
        incrementCounter.execute(flow1Stats);
    
        assertThat(flow1Counter.getAndReset(), is(1L));
        assertThat(flow1Counter.get(), is(0L));
        assertThat(counter.get(), is(1L));
    
        incrementCounter.execute(flow2Stats);
    
        assertThat(flow1Counter.get(), is(0L));
        assertThat(counter.get(), is(2L));
    }
  
    @Test
    public void appLevelAggregationCountersUnaffectdByClear()
    {
        ResetOnQueryCounter counter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
    
        incrementCounter.execute(flow1Stats);
        assertThat(counter.get(), is(1L));
    
        allStatistics.getApplicationStatistics().clear();
        assertThat(counter.get(), is(1L));
    
        incrementCounter.execute(flow2Stats);
        assertThat(counter.get(), is(2L));
    
        allStatistics.getApplicationStatistics().clear();
        assertThat(counter.get(), is(2L));
    }
  
    @Test
    public void appLevelAggregationCountersUnaffectdByFlowCountersClear()
    {
        ResetOnQueryCounter counter = (ResetOnQueryCounter)createCounter.transform(allStatistics.getApplicationStatistics());
    
        incrementCounter.execute(flow1Stats);
        assertThat(counter.get(), is(1L));
    
        flow1Stats.clear();
        assertThat(counter.get(), is(1L));
    
        incrementCounter.execute(flow2Stats);
        assertThat(counter.get(), is(2L));
    
        flow2Stats.clear();
        assertThat(counter.get(), is(2L));
    }
  
    @Test
    public void flowCountersUnaffectdByAppLevelAggregationCountersClear()
    {
        ResetOnQueryCounter counter1 = (ResetOnQueryCounter)createCounter.transform(flow1Stats);
        ResetOnQueryCounter counter2 = (ResetOnQueryCounter)createCounter.transform(flow2Stats);
    
        incrementCounter.execute(flow1Stats);
        assertThat(counter1.get(), is(1L));
        assertThat(counter2.get(), is(0L));
    
        allStatistics.clear();
        assertThat(counter1.get(), is(1L));
        assertThat(counter2.get(), is(0L));
    
        incrementCounter.execute(flow2Stats);
        assertThat(counter1.get(), is(1L));
        assertThat(counter2.get(), is(1L));
    
        allStatistics.clear();
        assertThat(counter1.get(), is(1L));
        assertThat(counter2.get(), is(1L));
    }
}
