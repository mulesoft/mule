package org.mule.management.stats;

import static org.junit.Assert.assertEquals;

import org.junit.*;
import org.mule.tck.junit4.AbstractMuleTestCase;

/**
 * Validates some basic assumptions about the ComponentStatistics class
 * behavior.
 */
public class ComponentStatisticsTestCase extends AbstractMuleTestCase {

    /* This class was added to address MULE-6417 */
    
    private String env_statIntervalTime;
    
    @Before
    public void setUp() {
        // ComponentStatistics reads statIntervalTime from the environment.
        // We will want to control it.
        env_statIntervalTime = System.getProperty("statIntervalTime");
        System.clearProperty("statIntervalTime");
    }
    
    @After
    public void tearDown() {
        if (env_statIntervalTime != null) {
            System.setProperty("statIntervalTime", env_statIntervalTime);
        }
    }
    
    private void assertValues(ComponentStatistics stats,
            long numEvents, long totalTime, long avgTime, 
            long maxTime, long minTime) {
        assertEquals("getExecutedEvents", numEvents, stats.getExecutedEvents());
        assertEquals("getTotalExecutionTime", totalTime, stats.getTotalExecutionTime());
        assertEquals("getAverageExecutionTime", avgTime, stats.getAverageExecutionTime());
        assertEquals("getMaxExecutionTime", maxTime, stats.getMaxExecutionTime());
        assertEquals("getMinExecutionTime", minTime, stats.getMinExecutionTime()); 
    }
    
    @Test
    public void testDefaults() {
        ComponentStatistics stats = new ComponentStatistics();
        // num, total, avg, max, min
        assertValues(stats, 0L, 0L, 0L, 0L, 0L);
        assertEquals(false, stats.isEnabled());
    }
    
    @Test
    public void testSingleEvent() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionTime(100L);
        // num, total, avg, max, min
        assertValues(stats, 1L, 100L, 100L, 100L, 100L);
    }
    
    @Test
    public void testSingleBranchEvent() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionBranchTime(true /*first*/, 25L, 25L);
        // num, total, avg, max, min
        assertValues(stats, 1L, 25L, 25L, 25L, 0L);
        stats.addExecutionBranchTime(false /*first*/, 25L, 50L);
        assertValues(stats, 1L, 50L, 50L, 50L, 0L);
        stats.addCompleteExecutionTime(50L);
        assertValues(stats, 1L, 50L, 50L, 50L, 50L);
    }
    
    @Test
    public void testClear() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionTime(100L);
        stats.clear();
        // num, total, avg, max, min
        assertValues(stats, 0L, 0L, 0L, 0L, 0L);
    }
    
    /* 
     * New behavior under the fix to MULE-6417 - no longer throws a 
     * divide-by-zero error. Instead, the remainder of the fragmented
     * event is ignored until a new event is started.
     * 
     * Note that this is a partial solution - if multiple components
     * are active at the same time, collection can be 're-enabled' 
     * for an already-started event. The established API does not 
     * allow for a solution, so for now this quirk must be accepted.
     */
    @Test
    public void testClearDuringBranch() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionBranchTime(true /*first*/, 25L, 25L);
        stats.clear();
        assertValues(stats, 0L, 0L, 0L, 0L, 0L);
        stats.addExecutionBranchTime(false /*first*/, 25L, 50L);
        assertValues(stats, 0L, 0L, 0L, 0L, 0L);
    }
    
    @Test
    public void testMaxMinAverage() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionTime(2L);
        stats.addExecutionTime(3L);
        // num, total, avg, max, min
        assertValues(stats, 2L, 5L, 2L /*note: floor*/, 3L, 2L);
    }
    
    @Test
    public void testBranchMaxMinAverage() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionBranchTime(true /*first*/, 2L, 2L);
        stats.addCompleteExecutionTime(2L);
        stats.addExecutionBranchTime(true /*first*/, 3L, 3L);
        stats.addCompleteExecutionTime(3L);
        // num, total, avg, max, min
        assertValues(stats, 2L, 5L, 2L /*note: floor*/, 3L, 2L);
    }

    @Test
    public void testMultiBranchMaxMinAverage() {
        ComponentStatistics stats = new ComponentStatistics();
        stats.addExecutionBranchTime(true /*first*/, 1L, 1L);
        stats.addExecutionBranchTime(false /*false*/, 1L, 2L);
        stats.addCompleteExecutionTime(2L);
        stats.addExecutionBranchTime(true /*first*/, 3L, 3L);
        stats.addCompleteExecutionTime(3L);
        // num, total, avg, max, min
        assertValues(stats, 2L, 5L, 2L /*note: floor*/, 3L, 2L);
    }
}
