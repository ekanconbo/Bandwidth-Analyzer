package directoryReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Justin Ross
 * 
 */

public class PropertyValues {
	String directory;
	String bar_chart_template;
	String bandwidth_profile;
	boolean generate_txt;
	boolean write_to_profile;
	boolean generate_bar_chart;

	InputStream inputStream;
	public PropertyValues() throws IOException {
		
		try {
			Properties properties = new Properties();
			String propertiesFileName = "config.properties";

			inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);

			if (inputStream != null) {
				properties.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propertiesFileName + "' not found in the classpath");
			}

			this.directory = properties.getProperty("directory");
			this.bar_chart_template = properties.getProperty("bar_chart_template");
			this.bandwidth_profile = properties.getProperty("bandwidth_profile");
			this.generate_txt = Boolean.valueOf(properties.getProperty("generate_txt"));
			this.write_to_profile = Boolean.valueOf(properties.getProperty("write_to_profile"));
			this.generate_bar_chart = Boolean.valueOf(properties.getProperty("generate_bar_chart"));
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
	}
}