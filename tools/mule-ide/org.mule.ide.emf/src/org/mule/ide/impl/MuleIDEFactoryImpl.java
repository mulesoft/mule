/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.ide.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.mule.ide.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class MuleIDEFactoryImpl extends EFactoryImpl implements MuleIDEFactory {
    /**
     * Creates an instance of the factory.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public MuleIDEFactoryImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EObject create(EClass eClass) {
        switch (eClass.getClassifierID()) {
            case MuleIDEPackage.CONFIG_FILE_REF_TYPE: return createConfigFileRefType();
            case MuleIDEPackage.CONFIG_FILE_TYPE: return createConfigFileType();
            case MuleIDEPackage.CONFIG_SET_TYPE: return createConfigSetType();
            case MuleIDEPackage.DOCUMENT_ROOT: return createDocumentRoot();
            case MuleIDEPackage.MULE_IDE_CONFIG_TYPE: return createMuleIdeConfigType();
            default:
                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ConfigFileRefType createConfigFileRefType() {
        ConfigFileRefTypeImpl configFileRefType = new ConfigFileRefTypeImpl();
        return configFileRefType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ConfigFileType createConfigFileType() {
        ConfigFileTypeImpl configFileType = new ConfigFileTypeImpl();
        return configFileType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ConfigSetType createConfigSetType() {
        ConfigSetTypeImpl configSetType = new ConfigSetTypeImpl();
        return configSetType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public DocumentRoot createDocumentRoot() {
        DocumentRootImpl documentRoot = new DocumentRootImpl();
        return documentRoot;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public MuleIdeConfigType createMuleIdeConfigType() {
        MuleIdeConfigTypeImpl muleIdeConfigType = new MuleIdeConfigTypeImpl();
        return muleIdeConfigType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public MuleIDEPackage getMuleIDEPackage() {
        return (MuleIDEPackage)getEPackage();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @deprecated
     * @generated
     */
    public static MuleIDEPackage getPackage() {
        return MuleIDEPackage.eINSTANCE;
    }

} //MuleIDEFactoryImpl
