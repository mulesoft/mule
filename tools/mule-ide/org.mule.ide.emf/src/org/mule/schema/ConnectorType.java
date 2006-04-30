/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Connector Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ConnectorType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getConnectionStrategy <em>Connection Strategy</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.ConnectorType#getRef <em>Ref</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getConnectorType()
 * @model extendedMetaData="name='connectorType' kind='mixed'"
 * @generated
 */
public interface ConnectorType extends EObject {
    /**
     * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
     * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Mixed</em>' attribute list.
     * @see org.mule.schema.MulePackage#getConnectorType_Mixed()
     * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
     *        extendedMetaData="kind='elementWildcard' name=':mixed'"
     * @generated
     */
    FeatureMap getMixed();

    /**
     * Returns the value of the '<em><b>Properties</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Properties</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Properties</em>' containment reference.
     * @see #setProperties(PropertiesType)
     * @see org.mule.schema.MulePackage#getConnectorType_Properties()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
     * @generated
     */
    PropertiesType getProperties();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getProperties <em>Properties</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Properties</em>' containment reference.
     * @see #getProperties()
     * @generated
     */
    void setProperties(PropertiesType value);

    /**
     * Returns the value of the '<em><b>Threading Profile</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Threading Profile</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Threading Profile</em>' containment reference.
     * @see #setThreadingProfile(ThreadingProfileType)
     * @see org.mule.schema.MulePackage#getConnectorType_ThreadingProfile()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='threading-profile' namespace='##targetNamespace'"
     * @generated
     */
    ThreadingProfileType getThreadingProfile();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getThreadingProfile <em>Threading Profile</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Threading Profile</em>' containment reference.
     * @see #getThreadingProfile()
     * @generated
     */
    void setThreadingProfile(ThreadingProfileType value);

    /**
     * Returns the value of the '<em><b>Exception Strategy</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Exception Strategy</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Exception Strategy</em>' containment reference.
     * @see #setExceptionStrategy(ExceptionStrategyType)
     * @see org.mule.schema.MulePackage#getConnectorType_ExceptionStrategy()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='exception-strategy' namespace='##targetNamespace'"
     * @generated
     */
    ExceptionStrategyType getExceptionStrategy();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getExceptionStrategy <em>Exception Strategy</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Exception Strategy</em>' containment reference.
     * @see #getExceptionStrategy()
     * @generated
     */
    void setExceptionStrategy(ExceptionStrategyType value);

    /**
     * Returns the value of the '<em><b>Connection Strategy</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Connection Strategy</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Connection Strategy</em>' containment reference.
     * @see #setConnectionStrategy(ConnectionStrategyType)
     * @see org.mule.schema.MulePackage#getConnectorType_ConnectionStrategy()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='connection-strategy' namespace='##targetNamespace'"
     * @generated
     */
    ConnectionStrategyType getConnectionStrategy();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getConnectionStrategy <em>Connection Strategy</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Connection Strategy</em>' containment reference.
     * @see #getConnectionStrategy()
     * @generated
     */
    void setConnectionStrategy(ConnectionStrategyType value);

    /**
     * Returns the value of the '<em><b>Class Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Class Name</em>' attribute.
     * @see #setClassName(String)
     * @see org.mule.schema.MulePackage#getConnectorType_ClassName()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='className' namespace='##targetNamespace'"
     * @generated
     */
    String getClassName();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getClassName <em>Class Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Class Name</em>' attribute.
     * @see #getClassName()
     * @generated
     */
    void setClassName(String value);

    /**
     * Returns the value of the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Name</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Name</em>' attribute.
     * @see #setName(String)
     * @see org.mule.schema.MulePackage#getConnectorType_Name()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
     * @generated
     */
    String getName();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Name</em>' attribute.
     * @see #getName()
     * @generated
     */
    void setName(String value);

    /**
     * Returns the value of the '<em><b>Ref</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Ref</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Ref</em>' attribute.
     * @see #setRef(String)
     * @see org.mule.schema.MulePackage#getConnectorType_Ref()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
     * @generated
     */
    String getRef();

    /**
     * Sets the value of the '{@link org.mule.schema.ConnectorType#getRef <em>Ref</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Ref</em>' attribute.
     * @see #getRef()
     * @generated
     */
    void setRef(String value);

} // ConnectorType
