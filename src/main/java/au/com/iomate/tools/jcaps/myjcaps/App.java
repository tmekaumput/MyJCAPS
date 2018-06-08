/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.iomate.tools.jcaps.myjcaps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author kmekaumput
 */
public class App {
    public static void main(String[] args) throws IOException, MalformedObjectNameException, Exception {
        String adminHost = args[0]; //"localhost";
        String adminPort = args[1]; //"20086";
        String user = args[2];
        String password = args[3];

        String jcapsSVGHome = args[4]; //"F:/apps/JavaCAPS63";
        String tempFolder = args[5] + adminHost + "-" + adminPort + "\\";//"C:\\Temp\\";
        File fTempFolder = new File(tempFolder);
        if (!fTempFolder.exists()) {
            fTempFolder.mkdirs();
        } else {
            fTempFolder.delete();
            fTempFolder.mkdirs();
        }


        JCAPSMBeanServerWrapper mbeanServerWrapper = new JCAPSMBeanServerWrapper(adminHost, Integer.parseInt(adminPort), user, password);
        mbeanServerWrapper.connect();

        Set<ObjectName> objectNames = mbeanServerWrapper.getMetaDataManagers();
        Map projectDataMap = new HashMap();

        for (ObjectName objName : objectNames) {
            String cmapXMLContent = mbeanServerWrapper.getConnectivityMap(objName);
            Document doc = XMLUtils.getDocument(cmapXMLContent);
            NodeList projectNodes = XMLUtils.extractProjectNodes(doc);
            for(int prjNodeIdx = 0; prjNodeIdx < projectNodes.getLength(); prjNodeIdx++) {
                Project project = new Project(projectNodes.item(prjNodeIdx), jcapsSVGHome);
                Map projectData = project.process(mbeanServerWrapper);
                projectDataMap.put(project.getProjectKey(), projectData);

            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(projectDataMap);
        FileWriter fJson = new FileWriter(tempFolder + "project.json");
        fJson.write(json);
        fJson.flush();
        fJson.close();
    }
}

