package directoryReader;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author Justin Ross
 * 
 * Questions? Contact me at:
 * Email: Rossj190@gmail.com
 * Phone: 225-328-1745
 */


public class DirectoryReader {

	public static boolean ContainsID(ArrayList<ExcelForm> list, String s){
		for(ExcelForm object : list){
			if(object.title.equals(s)){
				return true;
			}
		}
		return false;
	}


	public static void main(String[] args) throws Exception {
		try{
			//Gets all properties in config.properties
			PropertyValues properties = new PropertyValues();
			final File path = new File(properties.directory);

			ArrayList<ExcelForm> circuitArray = new ArrayList<ExcelForm>();
			for (File file : path.listFiles()) {
				System.out.println("\nReading files in: " + file.getAbsolutePath());
				for (File f : file.listFiles()) {

					String circuitID = null;

					String measuredPeriod = null;

					if (f.isFile() && f.getAbsolutePath().contains("html")) {
						System.out.println("Reading file: " + f.getAbsolutePath());


						circuitID = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("\\") +1, f.getAbsolutePath().indexOf("_")).trim();
						measuredPeriod = f.getAbsolutePath().substring(f.getAbsolutePath().indexOf("_")+1, f.getAbsolutePath().indexOf("."));



						//Creates a new object if one with the same circuit does not already exist
						if(!ContainsID(circuitArray, circuitID)){
							circuitArray.add(new ExcelForm(circuitID, properties.directory, properties.bandwidth_profile, properties.bar_chart_template));
						}

						//Parses HTML document
						Document doc = Jsoup.parse(f, "UTF-8", "");
						Element element1 = doc.select("div:contains(Variable)").get(0); //Grabs all values for RX and TX

						String billedBandwidth = String.valueOf((int)circuitArray.get(circuitArray.size() - 1).billedBandwidth*1000000);

						for(int i = 0; i != 2; i++){
							Element momentsTable = element1.select("table:contains(Moments)").get(i); //All RX values from moments

							String variable = element1.select("div:contains(Variable)").get(i+1).text(); //Should be "Variable: RX" or "Variable: TX"
							variable = variable.substring(variable.indexOf(":")+1).trim(); //Should be "RX or TX"

							Element quantilesTable = element1.select("table:contains(Quantiles)").get(i); //All RX and TX values from Quantiles

							String max = quantilesTable.select("th:contains(Max)").get(0).nextElementSibling().text();
							circuitArray.get(circuitArray.size() -1).LineInsert(new Value(max, variable, "max", measuredPeriod, circuitID));

							String ninetyFifthPercentile = quantilesTable.select("th:contains(95%)").get(0).nextElementSibling().text();
							circuitArray.get(circuitArray.size() -1).LineInsert(new Value(ninetyFifthPercentile, variable, "95%", measuredPeriod, circuitID));


							String mean = momentsTable.select("th:contains(Mean)").get(0).nextElementSibling().text(); //Mean for RX or TX
							circuitArray.get(circuitArray.size() -1).LineInsert(new Value(mean, variable, "mean", measuredPeriod, circuitID));



							String time = doc.select("td:contains(Cum.)").get(i).text();
							double cumPercent = 99999;
							double percent = 99999;
							Scanner scanner = new Scanner(time);
							while (scanner.hasNextLine()) {
								String line = scanner.nextLine();
								if(line.contains(billedBandwidth)){
									if(line.length() > line.substring(line.indexOf(billedBandwidth)).length()){
										if(line.substring(line.indexOf(billedBandwidth)-1, line.indexOf(billedBandwidth)+billedBandwidth.length()+1).trim().equals(billedBandwidth) ){
											percent = Double.parseDouble(line.substring(line.indexOf(".")-3, line.indexOf(".")+3));
											cumPercent = Double.parseDouble(line.substring(line.lastIndexOf(".")-3, line.lastIndexOf(".")+3));
											break;
										}

									}
									else if(line.length() == line.substring(line.indexOf(billedBandwidth)).length()){
										percent = Double.parseDouble(line.substring(line.indexOf(".")-3, line.indexOf(".")+3));
										cumPercent = Double.parseDouble(line.substring(line.lastIndexOf(".")-3, line.lastIndexOf(".")+3));
									}

								}
							}
							scanner.close();

							if(cumPercent <= 100 && percent <= 100){
								time = Double.toString(100 - cumPercent + percent);
							}
							else
								time = "N/A";


							circuitArray.get(circuitArray.size() -1).LineInsert(new Value(time, variable, "time", measuredPeriod, circuitID));


						}
					}

				}
				if(properties.generate_bar_chart){
					System.out.println("Generating Bar Chart for "+circuitArray.get(circuitArray.size()-1).title);
					circuitArray.get(circuitArray.size()-1).GenerateBarChart();
				}
				if(properties.write_to_profile){
					TimeUnit.MILLISECONDS.sleep(300);
					System.out.println("Writing data from "+circuitArray.get(circuitArray.size()-1).title + " to Bandwidth Profile");
					circuitArray.get(circuitArray.size()-1).WriteLine();
				}
				if(properties.generate_txt){
					System.out.println("Generating text file for "+circuitArray.get(circuitArray.size()-1).title);
					circuitArray.get(circuitArray.size()-1).GenerateLine();
				}
			}


		} catch (Exception e){
			e.printStackTrace();
		}			
	}
}

