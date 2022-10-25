package com.nextlabs.monitoring;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class Monitor {

	protected static final Logger logger = LogManager.getLogger(Monitor.class);
	private static Properties prop;
	private static int SLEEP_TIME = 600;
	private static String WATCHER_LIST = "";
	private static String QUEUE_JMX_URL = "";
	private final String URL_PREFIX = "service:jmx:rmi:///jndi/rmi://";
	private final String URL_SUFFIX = "/jmxrmi";
	private final int MB = 1024 * 1024;
	private static DecimalFormat df2 = new DecimalFormat(".##");
	
	
	
	public static String getPropertiesFile() {

		String path=new Monitor().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

		int startindex=0;

		if(path.startsWith("/") && OSUtils.isWindows()){
			startindex=1;
		}
		
		path=path.substring(startindex)+"/../config/config.properties";

		logger.debug("Monitor properties file with path-->" + path );

		return path;
	}
	
	public static Properties loadProperties(String name) {
		if (name == null)
			throw new IllegalArgumentException("null input: name");
		Properties result = null;
		try {
			File file = new File(name);
			logger.debug("Monitor: Properties File Path:: "
					+ file.getAbsolutePath());
			if (file != null) {
				FileInputStream fis = new FileInputStream(file);	
				result = new Properties();
				result.load(fis); // Can throw IOException
			}
		} catch (Exception e) {
			logger.error("Monitor: Error parsing properties file ", e);
			result = null;
		}
		return result;
	}
	
	public  void init(){
		
		prop = loadProperties(getPropertiesFile());
		SLEEP_TIME = Integer.parseInt(prop.getProperty("SLEEP_TIME"));
		QUEUE_JMX_URL = getFormattedURL(prop.getProperty("QUEUE_JMX_ADDRESS"));
		WATCHER_LIST = prop.getProperty("WATCHER_LIST");
		
	}
	
	
	private void showData() throws IOException {

		try {
			
			logger.info("\r\n-----------------------Start-------------------\r\n");
			readQueueDataJMX();
			
			readMemoryDataJMX();
			logger.info("\r\n------------------------End---------------------\r\n");
		} catch (MalformedObjectNameException e) {
			logger.error(e.toString(), e);
		} catch (NullPointerException e) {
			logger.error(e.toString(), e);
		} catch (AttributeNotFoundException e) {
			logger.error(e.toString(), e);
		} catch (InstanceNotFoundException e) {
			logger.error(e.toString(), e);
		} catch (MBeanException e) {
			logger.error(e.toString(), e);
		} catch (ReflectionException e) {
			logger.error(e.toString(), e);
		}

	}
	
	private String getFormattedURL(String sUrl){
		return URL_PREFIX + sUrl + URL_SUFFIX;
	}

	private void readMemoryDataJMX() throws IOException, MalformedObjectNameException, NullPointerException {
		
		String arrWatcherList[] = WATCHER_LIST.split(";");
		
		ObjectName objName = new ObjectName(
				ManagementFactory.MEMORY_MXBEAN_NAME);
		JMXServiceURL serviceURL;
		JMXConnector connector;
		MBeanServerConnection mbsc;
		
		for (String sWatcherURL : arrWatcherList) {
			
			serviceURL = new JMXServiceURL(getFormattedURL(sWatcherURL));
			connector = JMXConnectorFactory.connect(serviceURL);
			mbsc = connector.getMBeanServerConnection();
			
			String[] arrWatcherInfo = sWatcherURL.split(":");
			
			
			Set<ObjectName> mbeans = mbsc.queryNames(objName, null);
			
			StringBuffer sBuf;

			try {

				for (ObjectName name : mbeans) {

					MemoryMXBean memoryBean;

					memoryBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,name.toString(), MemoryMXBean.class);
					
					MemoryUsage heap = memoryBean.getHeapMemoryUsage();
					
					double lMaxHeap = heap.getMax() / MB * 1.00;
					double iMemoryUsage = (heap.getUsed()) / MB * 1.00;
					
					sBuf = new StringBuffer();

					sBuf.append("\r\n")
					    .append("============[").append(arrWatcherInfo[0]).append("]===============================").append("\r\n");
					sBuf.append("Memory heap usage : ").append(iMemoryUsage).append(" MB").append("\r\n");
					sBuf.append("Maximum heap      : ").append(lMaxHeap).append(" MB").append("\r\n");
					sBuf.append("Usage percentage  : ").append(df2.format((iMemoryUsage/lMaxHeap)* 100.00)).append(" %").append("\r\n");
					sBuf.append("============[").append(arrWatcherInfo[0]).append("]===============================").append("\r\n");
					
					logger.info(sBuf.toString());
					
				}

			} finally {
				logger.debug("Closing JMX connection.");
				connector.close();
			}
		
		}
	}
	
	private void readQueueDataJMX() throws IOException, MalformedObjectNameException, NullPointerException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {

		JMXServiceURL serviceURL = new JMXServiceURL(QUEUE_JMX_URL);
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
		MBeanServerConnection mbsc = connector.getMBeanServerConnection();

		ObjectName objName = new ObjectName("org.hornetq:module=JMS,type=Queue,name=\"NextlabsQueue\"");
		
		StringBuffer sBuf = new StringBuffer();

		try{

			long lTotalMessage  = (Long)mbsc.getAttribute(objName, "MessagesAdded"); 
			long lMessageinQueue  = (Long)mbsc.getAttribute(objName, "MessageCount");
			
			sBuf.append("\r\n")
		    	.append("============[").append(prop.getProperty("QUEUE_JMX_ADDRESS").split(":")[0]).append("]===============================").append("\r\n");
			sBuf.append("Total Message received from Watcher : ").append(lTotalMessage).append("\r\n");
			sBuf.append("Pending Message count in Queue      : ").append(lMessageinQueue).append("\r\n");
			sBuf.append("============[").append(prop.getProperty("QUEUE_JMX_ADDRESS").split(":")[0]).append("]===============================").append("\r\n");
			
			logger.info(sBuf);
		}
		finally{
			logger.debug("Closing JMX connection.");
			connector.close();
		}
	}
	
	
	/*
	 * Generate current time in format compliant.
	 */
	public static String getFormatedTime() {
		Date date = new Date();
		DateFormat zuluTime = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'");
		zuluTime.setTimeZone(TimeZone.getTimeZone("UTC"));
		return zuluTime.format(date);
	}
	
	
	public static void main(String args[]) {
		
		logger.info("Starting....");
		Monitor attc = new Monitor();
		attc.init();

		for (;;) {

			try {
				attc.showData();

				Thread.currentThread();
				Thread.sleep(SLEEP_TIME);

			} catch (Exception e) {
				logger.error(e.toString(),e);
			}
		}

	}
}