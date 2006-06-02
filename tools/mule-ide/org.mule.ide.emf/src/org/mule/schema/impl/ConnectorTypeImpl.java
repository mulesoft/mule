/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.ConnectionStrategyType;
import org.mule.schema.ConnectorType;
import org.mule.schema.ExceptionStrategyType;
import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.ThreadingProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Connector Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getConnectionStrategy <em>Connection Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConnectorTypeImpl#getRef <em>Ref</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ConnectorTypeImpl extends EObjectImpl implements ConnectorType {
    /**
     * The cached value of the '{@link #getMixed() <em>Mixed</em>}' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getMixed()
     * @generated
     * @ordered
     */
    protected FeatureMap mixed = null;

    /**
     * The default value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getClassName()
     * @generated
     * @ordered
     */
    protected static final String CLASS_NAME_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getClassName()
     * @generated
     * @ordered
     */
    protected String className = CLASS_NAME_EDEFAULT;

    /**
     * The default value of the '{@link #getName() <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getName()
     * @generated
     * @ordered
     */
    protected static final String NAME_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getName()
     * @generated
     * @ordered
     */
    protected String name = NAME_EDEFAULT;

    /**
     * The default value of the '{@link #getRef() <em>Ref</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getRef()
     * @generated
     * @ordered
     */
    protected static final String REF_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getRef() <em>Ref</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getRef()
     * @generated
     * @ordered
     */
    protected String ref = REF_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected ConnectorTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected EClass eStaticClass() {
        return MulePackage.eINSTANCE.getConnectorType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public FeatureMap getMixed() {
        if (mixed == null) {
            mixed = new BasicFeatureMap(this, MulePackage.CONNECTOR_TYPE__MIXED);
        }
        return mixed;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public PropertiesType getProperties() {
        return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getConnectorType_Properties(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConnectorType_Properties(), newProperties, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setProperties(PropertiesType newProperties) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConnectorType_Properties(), newProperties);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ThreadingProfileType getThreadingProfile() {
        return (ThreadingProfileType)getMixed().get(MulePackage.eINSTANCE.getConnectorType_ThreadingProfile(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetThreadingProfile(ThreadingProfileType newThreadingProfile, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConnectorType_ThreadingProfile(), newThreadingProfile, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setThreadingProfile(ThreadingProfileType newThreadingProfile) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConnectorType_ThreadingProfile(), newThreadingProfile);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ExceptionStrategyType getExceptionStrategy() {
        return (ExceptionStrategyType)getMixed().get(MulePackage.eINSTANCE.getConnectorType_ExceptionStrategy(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetExceptionStrategy(ExceptionStrategyType newExceptionStrategy, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConnectorType_ExceptionStrategy(), newExceptionStrategy, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setExceptionStrategy(ExceptionStrategyType newExceptionStrategy) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConnectorType_ExceptionStrategy(), newExceptionStrategy);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public ConnectionStrategyType getConnectionStrategy() {
        return (ConnectionStrategyType)getMixed().get(MulePackage.eINSTANCE.getConnectorType_ConnectionStrategy(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetConnectionStrategy(ConnectionStrategyType newConnectionStrategy, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConnectorType_ConnectionStrategy(), newConnectionStrategy, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setConnectionStrategy(ConnectionStrategyType newConnectionStrategy) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConnectorType_ConnectionStrategy(), newConnectionStrategy);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getClassName() {
        return className;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setClassName(String newClassName) {
        String oldClassName = className;
        className = newClassName;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONNECTOR_TYPE__CLASS_NAME, oldClassName, className));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getName() {
        return name;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setName(String newName) {
        String oldName = name;
        name = newName;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONNECTOR_TYPE__NAME, oldName, name));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getRef() {
        return ref;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setRef(String newRef) {
        String oldRef = ref;
        ref = newRef;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONNECTOR_TYPE__REF, oldRef, ref));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
        if (featureID >= 0) {
            switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
                case MulePackage.CONNECTOR_TYPE__MIXED:
                    return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
                case MulePackage.CONNECTOR_TYPE__PROPERTIES:
                    return basicSetProperties(null, msgs);
                case MulePackage.CONNECTOR_TYPE__THREADING_PROFILE:
                    return basicSetThreadingProfile(null, msgs);
                case MulePackage.CONNECTOR_TYPE__EXCEPTION_STRATEGY:
                    return basicSetExceptionStrategy(null, msgs);
                case MulePackage.CONNECTOR_TYPE__CONNECTION_STRATEGY:
                    return basicSetConnectionStrategy(null, msgs);
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
            case MulePackage.CONNECTOR_TYPE__MIXED:
                return getMixed();
            case MulePackage.CONNECTOR_TYPE__PROPERTIES:
                return getProperties();
            case MulePackage.CONNECTOR_TYPE__THREADING_PROFILE:
                return getThreadingProfile();
            case MulePackage.CONNECTOR_TYPE__EXCEPTION_STRATEGY:
                return getExceptionStrategy();
            case MulePackage.CONNECTOR_TYPE__CONNECTION_STRATEGY:
                return getConnectionStrategy();
            case MulePackage.CONNECTOR_TYPE__CLASS_NAME:
                return getClassName();
            case MulePackage.CONNECTOR_TYPE__NAME:
                return getName();
            case MulePackage.CONNECTOR_TYPE__REF:
                return getRef();
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
            case MulePackage.CONNECTOR_TYPE__MIXED:
                getMixed().clear();
                getMixed().addAll((Collection)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__PROPERTIES:
                setProperties((PropertiesType)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__THREADING_PROFILE:
                setThreadingProfile((ThreadingProfileType)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__EXCEPTION_STRATEGY:
                setExceptionStrategy((ExceptionStrategyType)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__CONNECTION_STRATEGY:
                setConnectionStrategy((ConnectionStrategyType)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__CLASS_NAME:
                setClassName((String)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__NAME:
                setName((String)newValue);
                return;
            case MulePackage.CONNECTOR_TYPE__REF:
                setRef((String)newValue);
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
            case MulePackage.CONNECTOR_TYPE__MIXED:
                getMixed().clear();
                return;
            case MulePackage.CONNECTOR_TYPE__PROPERTIES:
                setProperties((PropertiesType)null);
                return;
            case MulePackage.CONNECTOR_TYPE__THREADING_PROFILE:
                setThreadingProfile((ThreadingProfileType)null);
                return;
            case MulePackage.CONNECTOR_TYPE__EXCEPTION_STRATEGY:
                setExceptionStrategy((ExceptionStrategyType)null);
                return;
            case MulePackage.CONNECTOR_TYPE__CONNECTION_STRATEGY:
                setConnectionStrategy((ConnectionStrategyType)null);
                return;
            case MulePackage.CONNECTOR_TYPE__CLASS_NAME:
                setClassName(CLASS_NAME_EDEFAULT);
                return;
            case MulePackage.CONNECTOR_TYPE__NAME:
                setName(NAME_EDEFAULT);
                return;
            case MulePackage.CONNECTOR_TYPE__REF:
                setRef(REF_EDEFAULT);
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
            case MulePackage.CONNECTOR_TYPE__MIXED:
                return mixed != null && !mixed.isEmpty();
            case MulePackage.CONNECTOR_TYPE__PROPERTIES:
                return getProperties() != null;
            case MulePackage.CONNECTOR_TYPE__THREADING_PROFILE:
                return getThreadingProfile() != null;
            case MulePackage.CONNECTOR_TYPE__EXCEPTION_STRATEGY:
                return getExceptionStrategy() != null;
            case MulePackage.CONNECTOR_TYPE__CONNECTION_STRATEGY:
                return getConnectionStrategy() != null;
            case MulePackage.CONNECTOR_TYPE__CLASS_NAME:
                return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
            case MulePackage.CONNECTOR_TYPE__NAME:
                return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
            case MulePackage.CONNECTOR_TYPE__REF:
                return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
        }
        return eDynamicIsSet(eFeature);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String toString() {
        if (eIsProxy()) return super.toString();

        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (mixed: ");
        result.append(mixed);
        result.append(", className: ");
        result.append(className);
        result.append(", name: ");
        result.append(name);
        result.append(", ref: ");
        result.append(ref);
        result.append(')');
        return result.toString();
    }

} //ConnectorTypeImpl
