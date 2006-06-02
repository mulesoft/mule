/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.ide.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.ide.ConfigFileType;
import org.mule.ide.ConfigSetType;
import org.mule.ide.MuleIDEPackage;
import org.mule.ide.MuleIdeConfigType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Mule Ide Config Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.ide.impl.MuleIdeConfigTypeImpl#getConfigFile <em>Config File</em>}</li>
 *   <li>{@link org.mule.ide.impl.MuleIdeConfigTypeImpl#getConfigSet <em>Config Set</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MuleIdeConfigTypeImpl extends EObjectImpl implements MuleIdeConfigType {
    /**
     * The cached value of the '{@link #getConfigFile() <em>Config File</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getConfigFile()
     * @generated
     * @ordered
     */
    protected EList configFile = null;

    /**
     * The cached value of the '{@link #getConfigSet() <em>Config Set</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getConfigSet()
     * @generated
     * @ordered
     */
    protected EList configSet = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected MuleIdeConfigTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected EClass eStaticClass() {
        return MuleIDEPackage.eINSTANCE.getMuleIdeConfigType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList getConfigFile() {
        if (configFile == null) {
            configFile = new EObjectContainmentEList(ConfigFileType.class, this, MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE);
        }
        return configFile;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList getConfigSet() {
        if (configSet == null) {
            configSet = new EObjectContainmentEList(ConfigSetType.class, this, MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET);
        }
        return configSet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
        if (featureID >= 0) {
            switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
                case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE:
                    return ((InternalEList)getConfigFile()).basicRemove(otherEnd, msgs);
                case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET:
                    return ((InternalEList)getConfigSet()).basicRemove(otherEnd, msgs);
                default:
                    return eDynamicInverseRemove(otherEnd, featureID, baseClass, msgs);
            }
        }
        return eBasicSetContainer(null, featureID, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public Object eGet(EStructuralFeature eFeature, boolean resolve) {
        switch (eDerivedStructuralFeatureID(eFeature)) {
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE:
                return getConfigFile();
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET:
                return getConfigSet();
        }
        return eDynamicGet(eFeature, resolve);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void eSet(EStructuralFeature eFeature, Object newValue) {
        switch (eDerivedStructuralFeatureID(eFeature)) {
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE:
                getConfigFile().clear();
                getConfigFile().addAll((Collection)newValue);
                return;
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET:
                getConfigSet().clear();
                getConfigSet().addAll((Collection)newValue);
                return;
        }
        eDynamicSet(eFeature, newValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void eUnset(EStructuralFeature eFeature) {
        switch (eDerivedStructuralFeatureID(eFeature)) {
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE:
                getConfigFile().clear();
                return;
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET:
                getConfigSet().clear();
                return;
        }
        eDynamicUnset(eFeature);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public boolean eIsSet(EStructuralFeature eFeature) {
        switch (eDerivedStructuralFeatureID(eFeature)) {
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_FILE:
                return configFile != null && !configFile.isEmpty();
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE__CONFIG_SET:
                return configSet != null && !configSet.isEmpty();
        }
        return eDynamicIsSet(eFeature);
    }

} //MuleIdeConfigTypeImpl
