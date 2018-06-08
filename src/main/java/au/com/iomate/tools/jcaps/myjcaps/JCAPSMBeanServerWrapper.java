/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.iomate.tools.jcaps.myjcaps;

/**
 *
 * @author kmekaumput
 */

import com.stc.codegen.framework.metadata.base.MetaDataObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 * @author kmekaumput
 */
class JCAPSMBeanServerWrapper {

    private String hostname = null;
    private int port = -1;
    private String username = null;
    private String password = null;
    private MBeanServerConnection connection = null;
    private String CONNECTION_PREFIX = "service:jmx:rmi:///jndi/rmi://";
    private String CONNECTION_SUFFIX = "/management/rmi-jmx-connector";

    JCAPSMBeanServerWrapper(String hostname, int port, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void registerListener(ObjectName objectName, NotificationListener listener) throws InstanceNotFoundException, IOException {
        connection.addNotificationListener(objectName, listener, null, null);
        
    }

    public void connect() throws Exception {
        Map environment = new HashMap();
        String[] credentials = new String[]{getUsername(), getPassword()};
        environment.put(JMXConnector.CREDENTIALS, credentials);

        JMXServiceURL url = new JMXServiceURL(CONNECTION_PREFIX + getHostname() + ":" + getPort() + CONNECTION_SUFFIX);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, environment);
        setConnection(jmxc.getMBeanServerConnection());


    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the connection
     */
    public MBeanServerConnection getConnection() {
        return connection;
    }

    /**
     * @param connection the connection to set
     */
    public void setConnection(MBeanServerConnection connection) {
        this.connection = connection;
    }

    public Set<ObjectName> getMetaDataManagers() throws IOException, MalformedObjectNameException {
        ObjectName metaDataManagerName = new ObjectName("SeeBeyond:type=MetaDataManager,*");
        Set<ObjectName> objectNames = getConnection().queryNames(metaDataManagerName, null);
        return objectNames;
    }

    public ObjectInstance getObjectInstance(ObjectName objName) {
        ObjectInstance objInst = null;
        try {
            objInst = connection.getObjectInstance(objName);
        } catch (InstanceNotFoundException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return objInst;
    }

    public MBeanInfo getMBeanInfo(ObjectName objName) {
        MBeanInfo mbeanInfo = null;
        try {
            mbeanInfo = connection.getMBeanInfo(objName);
        } catch (IntrospectionException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstanceNotFoundException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ReflectionException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mbeanInfo;
    }

    public String getConnectivityMap(ObjectName objName) throws Exception {
        try {
            MetaDataObject cmapMetaData =
                    (MetaDataObject) connection.invoke(objName, "getMetaDataObject", new Object[]{"em/EM_ConnectivityMap.xml"}, new String[]{java.lang.String.class.getName()});

            /*
            MetaDataObject dpMetaData =
            (MetaDataObject) connection.invoke(objName, "getMetaDataObject", new Object[]{"em/DeploymentPlan.xml"}, new String[]{java.lang.String.class.getName()});
             */

            String cmapXMLContent = cmapMetaData.convertToXMLString();
            return cmapXMLContent;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String getAttributeString(ObjectName objName, String attributeName) throws Exception{
        try {
            String value = (String) connection.getAttribute(objName, attributeName);
            return value;
        } catch (MBeanException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (AttributeNotFoundException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (InstanceNotFoundException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (ReflectionException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (IOException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    public Object getAttributeObject(ObjectName objName, String attributeName) throws Exception{
        try {
            Object value = connection.getAttribute(objName, attributeName);
            return value;
        } catch (MBeanException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (AttributeNotFoundException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (InstanceNotFoundException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (ReflectionException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (IOException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    public Object invokeMethod(ObjectName objName, String methodName, Object[] paramValues, String[] paramTypes) throws Exception{
        try {
            Object value = connection.invoke(objName, methodName, paramValues, paramTypes);
            return value;
        } catch (MBeanException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (InstanceNotFoundException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (ReflectionException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (IOException ex) {
            //Logger.getLogger(JCAPSMBeanServerWrapper.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }
    //Set<ObjectName> objectNames = connection.queryNames(ObjectName.WILDCARD, null);
}
