/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.object;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationCallback;

/**
 * <code>ObjectFactory</code> is a generic Factory interface.
 */
public interface ObjectFactory extends Initialisable, Disposable
{
    /**
     * Retrieve an instance of the object. This may create a new instance or look up
     * an existing instance depending on the implementation. If a new instance is
     * created it will also be initialized by this method
     * (Initilisable.initialise()).
     * @param muleContext the current {@link org.mule.api.MuleContext} instance. This can be used for performing registry look-ups
     * applying processors to newly created objects or even firing custom notifications
     * @throws Exception if there is an exception thrown creating the new instance
     * @return A new instance of an object.  The factory may decide to return the same instance each type or create a new instance each time
     */
    Object getInstance(MuleContext muleContext) throws Exception;

    /**
     * Returns the class of the object to be instantiated without actually creating
     * an instance. This may not be logical or even possible depending on the
     * implementation.
     */
    Class<?> getObjectClass();

    /**
     * Returns true if the ObjectFactory implementation always returns the same object
     * instance.
     */
    boolean isSingleton();

    /**
     * Returns true if Mule should not manage the life-cycle the object instance returned from the ObjectFactory.
     * This is normally false except when an ObjectFactory implementation obtains instance from containers
     * (e.g. Spring) that already manages the objects lifecycle.
     * instance.
     */
    boolean isExternallyManagedLifecycle();

    /**
     * Return true if the created object should get its dependencies wired from the registry automatically.  Typically
     * Mule object factories would return true for this value, objects managed by DI container such as Spring should
     * set this to false.
     */
    boolean isAutoWireObject();

    /**
     * Register a custom initialiser
     */
    void addObjectInitialisationCallback(InitialisationCallback callback);

}
