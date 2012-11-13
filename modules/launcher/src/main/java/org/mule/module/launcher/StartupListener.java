package org.mule.module.launcher;

/**
*  Notifies when all mule apps has been started
*/
public interface StartupListener
{

    /**
     * Invoked after all apps have passed the deployment phase. Any exceptions thrown by implementations
     * will be ignored.
     */
    void onAfterStartup();
}
