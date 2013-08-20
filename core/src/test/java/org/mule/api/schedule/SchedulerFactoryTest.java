package org.mule.api.schedule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.registry.MuleRegistry;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Test to validate the interface {@link SchedulerFactory} interface
 */
public class SchedulerFactoryTest
{

    public static final String NAME = "test";
    private Scheduler mockScheduler = mock(Scheduler.class);
    private SchedulerFactoryPostProcessor postProcessor1 = mock(SchedulerFactoryPostProcessor.class);
    private MuleContext muleContext = mock(MuleContext.class);
    private MuleRegistry muleRegistry = mock(MuleRegistry.class);
    private SchedulerFactoryPostProcessor postProcessor2 = mock(SchedulerFactoryPostProcessor.class);


    /**
     * If the {@link Scheduler} created is null then throw an {@link SchedulerCreationException}
     */
    @Test(expected = SchedulerCreationException.class)
    public void checkCreationOfNullScheduler()
    {
        factory(null, null).create(NAME,new Object());
    }

    /**
     * If the {@link Scheduler} post processed is null then throw {@link SchedulerCreationException}
     */
    @Test(expected = SchedulerCreationException.class)
    public void postProcessMethodMustNeverReturnANullScheduler()
    {
        commonMockBehaviour(singlePostProcessor());

        factory(mockScheduler, muleContext).create(NAME, new Object());
    }


    /**
     * If the {@link Scheduler} post processed is null then throw {@link SchedulerCreationException}
     */
    @Test(expected = SchedulerCreationException.class)
    public void postProcessorMustNeverReturnANullScheduler()
    {
        commonMockBehaviour(postProcessors());

        SchedulerFactory factory = factory(mockScheduler, muleContext);

        try
        {
            factory.create(NAME,new Object());
        }
        finally
        {
            verify(postProcessor2, never()).process(null, null);
            verify(postProcessor1, never()).process(null, null);
        }

    }

    /**
     * If the {@link MuleContext} is null the post processing is never done
     */
    @Test
    public void muleContextIsNullThenAvoidPostProcessing()
    {
        commonMockBehaviour(postProcessors());
        Object job = new Object();

        SchedulerFactory factory = factory(mockScheduler, null);

        try
        {
            assertEquals(mockScheduler, factory.create(NAME, job));
        }
        finally
        {
            verify(postProcessor2, never()).process(job,null);
            verify(postProcessor1, never()).process(job,null);
        }
    }

    /**
     * Happy path
     */
    @Test
    public void createTheScheduler()
    {
        Object job = new Object();
        commonMockBehaviour(singlePostProcessor());
        when(postProcessor1.process(job,mockScheduler)).thenReturn(mockScheduler);

        assertEquals(mockScheduler, factory(mockScheduler, muleContext).create(NAME,job));
    }

    private Map<String, SchedulerFactoryPostProcessor> postProcessors()
    {
        Map<String, SchedulerFactoryPostProcessor> registeredPostProcessors = singlePostProcessor();
        registeredPostProcessors.put("postProcessor2", postProcessor2);
        return registeredPostProcessors;
    }

    private void commonMockBehaviour(Map<String, SchedulerFactoryPostProcessor> registeredPostProcessors)
    {
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.lookupByType(SchedulerFactoryPostProcessor.class)).thenReturn(registeredPostProcessors);
    }

    private Map<String, SchedulerFactoryPostProcessor> singlePostProcessor()
    {
        Map<String, SchedulerFactoryPostProcessor> registeredPostProcessors = new HashMap<String, SchedulerFactoryPostProcessor>();
        registeredPostProcessors.put("postProcessor1", postProcessor1);
        return registeredPostProcessors;
    }

    private SchedulerFactory factory(Scheduler schedulerToReturn, MuleContext muleContext)
    {
        TestedSchedulerFactory factory = new TestedSchedulerFactory(schedulerToReturn);
        factory.setMuleContext(muleContext);
        return factory;
    }

    private class TestedSchedulerFactory extends SchedulerFactory<Object>
    {

        private Scheduler schedulerToReturn;

        private TestedSchedulerFactory(Scheduler schedulerToReturn)
        {
            this.schedulerToReturn = schedulerToReturn;
        }

        @Override
        protected Scheduler doCreate(String name, Object job)
        {
            return schedulerToReturn;
        }
    }


}
