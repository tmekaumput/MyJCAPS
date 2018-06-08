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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stc.codegen.framework.metadata.base.MetaDataObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.misc.BASE64Decoder;

/**
 *
 * @author kmekaumput
 */
public class Project {
    private Node projectNode;
    private String jcapsSVGHome;
    private String name;
    private String deployment;
    private String environment;
    private String projectPath;
    private String projectKey;

    public Project(Node projectNode, String jcapsSVGHome) {
        this.projectNode = projectNode;
        this.jcapsSVGHome = jcapsSVGHome;
        populateAttributes(projectNode);
    }

    private void populateAttributes(Node projectNode) {
        NamedNodeMap attrMap = projectNode.getAttributes();
        this.setName(attrMap.getNamedItem("name").getTextContent());
        this.setDeployment(attrMap.getNamedItem("deployment").getTextContent());
        this.setEnvironment(attrMap.getNamedItem("environment").getTextContent());
        this.setProjectPath(attrMap.getNamedItem("projectPath").getTextContent());
        this.setProjectKey(getEnvironment() + "-" + getProjectPath() + "." + getDeployment());
    }

    public Map process(JCAPSMBeanServerWrapper jcapsWrapper) {

            //Node prjNode = getProjectNode();

            //String name = prjNode.getAttributes().getNamedItem("name").getTextContent();
            //String environment = prjNode.getAttributes().getNamedItem("environment").getTextContent();
            //String projectPath = prjNode.getAttributes().getNamedItem("projectPath").getTextContent().replace("|", ".").replace("/", ".");
            //String deployment = prjNode.getAttributes().getNamedItem("deployment").getTextContent();
            //String prjKey = environment + "-" + projectPath + "." + deployment;
            //Map<String, Map> projDataMap = prjMap.get(prjKey);
            //if (projDataMap == null) {
            //    projDataMap = new TreeMap<String, Map>();
            //    prjMap.put(prjKey, projDataMap);
            //}
            Properties projProperties = new Properties();

            projProperties.put("DEPLOYMENT", deployment);
            projProperties.put("ENVIRONMENT", environment);
            projProperties.put("NAME", name);
            projProperties.put("PROJECT_PATH", projectPath);

            Map cmapDataMap = new HashMap();
            projProperties.put("CMAPS", cmapDataMap);

            //projDataMap.put("PROPERTIES", projProperties);

            List<Node> cmapNodes = getCMAPNodes();


            for (Node cmapNode : cmapNodes) {
                //System.out.println(cmapNode.getLocalName());
                CMAP cmap = new CMAP(this, cmapNode);
                try {
                    Map cmapData = cmap.process(jcapsWrapper);
                    cmapDataMap.put(cmap.getName(), cmapData);
                    //processCmaps(cmapNode, projDataMap, jc);
                    //processSVG(cmapNode, projDataMap, jcapsSVGHome, connection);
                } catch (IOException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MalformedObjectNameException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstanceNotFoundException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MBeanException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (AttributeNotFoundException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ReflectionException ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return projProperties;
    }

    /**
     * @return the projectNode
     */
    public Node getProjectNode() {
        return projectNode;
    }

    /**
     * @param projectNode the projectNode to set
     */
    public void setProjectNode(Node projectNode) {
        this.projectNode = projectNode;
    }

    /**
     * @return the jcapsSVGHome
     */
    public String getJcapsSVGHome() {
        return jcapsSVGHome;
    }

    /**
     * @param jcapsSVGHome the jcapsSVGHome to set
     */
    public void setJcapsSVGHome(String jcapsSVGHome) {
        this.jcapsSVGHome = jcapsSVGHome;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the deployment
     */
    public String getDeployment() {
        return deployment;
    }

    /**
     * @param deployment the deployment to set
     */
    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    /**
     * @return the environment
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * @param environment the environment to set
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * @return the projectPath
     */
    public String getProjectPath() {
        return projectPath;
    }

    /**
     * @param projectPath the projectPath to set
     */
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * @return the projectKey
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * @param projectKey the projectKey to set
     */
    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }


    public List getCMAPNodes() {
        List cmapNodeList = new ArrayList();
        for (int chNodeIndex = 0; chNodeIndex < getProjectNode().getChildNodes().getLength(); chNodeIndex++) {
            if (getProjectNode().getChildNodes().item(chNodeIndex).getLocalName().equals("ConnectivityMap")) {
                cmapNodeList.add(getProjectNode());
            }
        }
        return cmapNodeList;
    }

}
