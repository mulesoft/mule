package org.mule.api.processor;

/**
 * This interface defines a contract for a component able to name staged queues
 * through a {@link org.mule.api.processor.StageNameSource} implementation
 *
 * @since 3.5.0
 */
public interface StageNameSourceProvider
{

    /**
     * Provides a {@link org.mule.api.processor.StageNameSource}
     *
     * @return a {@link org.mule.api.processor.StageNameSource}
     */
    public StageNameSource getAsyncStageNameSource();

    /**
     * Returns a {@link org.mule.api.processor.StageNameSource} that
     * takes the given paramter into consideration when generating the name
     *
     * @param asyncName a name to be consider when building the final name
     * @return a {@link org.mule.api.processor.StageNameSource}
     */
    public StageNameSource getAsyncStageNameSource(String asyncName);

}
