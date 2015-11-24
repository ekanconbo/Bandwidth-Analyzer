package directoryReader;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.opencsv.CSVReader;

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

	//Analyzes CSV. Looks at a time period (24-7, M-F, M-F_8-5) and returns a list of Value objects.
	public static List<Value> CSVAnalyze(File f, Long billedBandwidth, String measuredPeriod, String circuitID, boolean generate_list) throws IOException{
		List<Value> l = new ArrayList<Value>();
		
		//Reads the csv and separates values based on commas and tabs.
		CSVReader reader = new CSVReader(new FileReader(f), ',', '\t', 1);
		
		String[] nextLine = reader.readNext();
		
		//This is the 4th value of the 2nd line in the CSV file, which is the circuit name
		String circuit = nextLine[3];
		
		List<Long> rxList = new ArrayList<Long>();
		List<Long> txList = new ArrayList<Long>();

		long rxMax = 0, txMax = 0;
		double rxAverage = 0.0, txAverage = 0.0;
		while (nextLine != null) {
			System.out.println("Circuit: " + nextLine[4].toUpperCase());
			for(int i = 21; i < nextLine.length; i++)
			{
				{
					//Checks if the list already contains a value at that point. if it doesn't, add a new element with the value.
					//If it does contain a value, add the value we are inserting to the value already present there.
					if(nextLine[4].equals("rx"))
					{

						if(rxList.size() <= i-21)
						{
							if (!nextLine[i].equals(""))
							{
								rxList.add(Long.parseLong(nextLine[i]));
							}
							else
								rxList.add((long)0);
						}

						else
						{
							if (!nextLine[i].equals(""))
							{

								rxList.set(i-21, Long.parseLong(nextLine[i])+rxList.get(i-21));
							}
						}
					}
					else if(nextLine[4].equals("tx"))
					{
						if(txList.size() <= i-21)
						{
							if (!nextLine[i].equals(""))
								txList.add(Long.parseLong(nextLine[i]));
							else
								txList.add((long)0);
						}

						else
						{
							if (!nextLine[i].equals(""))
								txList.set(i-21, Long.parseLong(nextLine[i])+txList.get(i-21));
						}
					}
				}
			}
			nextLine = reader.readNext();
		}
		reader.close();

		//Sorts the list, then removes all 0 elements from the list
		Collections.sort(rxList);
		for(int i = 0; i != rxList.size(); i++)
		{
			if(rxList.get(i) != 0)
			{
				rxList = rxList.subList(i, rxList.size());
				break;
			}
		}
		rxMax = rxList.get(rxList.size()-1);

		Collections.sort(txList);
		for(int i = 0; i != txList.size(); i++)
		{
			if(txList.get(i) != 0)
			{
				txList = txList.subList(i, txList.size());
				break;
			}
		}
		txMax = txList.get(txList.size()-1);

		
		//Creates two txt files that contain the values in the csv sorted.
		if(generate_list)
		{
			System.out.println("Writing lists in " + (f.getAbsolutePath().substring(0 ,f.getAbsolutePath().lastIndexOf("\\"))));
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath().substring(0 ,f.getAbsolutePath().lastIndexOf("\\")) + "/"+circuitID+"_"+measuredPeriod+"_RX.txt"), "utf-8"))) {
				for(int i = 0; i != rxList.size(); i++){
					writer.write("\"" + (i+1) + "\":\t" + rxList.get(i)+"\n");
				}
			} catch(IOException ex){
				ex.printStackTrace();
			}

			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath().substring(0 ,f.getAbsolutePath().lastIndexOf("\\")) + "/"+circuitID+"_"+measuredPeriod+"_TX.txt"), "utf-8"))) {
				for(int i = 0; i != txList.size(); i++){
					writer.write("\"" + (i+1) + "\":\t" + txList.get(i)+"\n");
				}
			} catch(IOException ex){
				ex.printStackTrace();
			}
		}

		int rxOver = 0, txOver = 0;

		//Checks each value to see if it's greater than the billed bandwidth. This is how we calculate time.
		for(Long data : rxList)
		{
			rxAverage += ((double)data/rxList.size());
			if(data >= billedBandwidth)
			{
				rxOver++;
			}
		}

		for(Long data : txList)
		{
			txAverage += ((double)data/rxList.size());
			if(data >= billedBandwidth)
			{
				txOver++;
			}
		}

		
		int rx95Point = 0, tx95Point = 0;
		long rx95Percentile = 0, tx95Percentile = 0;

		//Calculates the 95th percentile.
		//If the number of elements * .95 is not a whole number, we round it up, and get the value at that position-1 (because the array is 0 indexed)
		//If the number is whole, we get the value at position - 1 and the value at the next position, then average them.
		if(rxList.size()*.95 != Math.ceil(rxList.size()*.95))
		{
			rx95Point = (int) Math.ceil((rxList.size())*.95)-1;
			rx95Percentile = rxList.get(rx95Point);
		}
		else
		{
			rx95Point = (int)(rxList.size()*.95)-1;
			rx95Percentile = (long)((rxList.get(rx95Point) + rxList.get(rx95Point + 1)) / 2.0);
		}

		if(txList.size()*.95 != Math.ceil(txList.size()*.95))
		{
			tx95Point = (int) Math.ceil((txList.size())*.95)-1;
			tx95Percentile = txList.get(tx95Point);
		}
		else
		{
			tx95Point = (int)(txList.size()*.95)-1;
			tx95Percentile = (long)((txList.get(tx95Point) + txList.get(tx95Point + 1)) / 2.0);
		}


		System.out.println("rxlist size is " +(rxList.size()));
		System.out.println("txlist size is " +(txList.size()));
		System.out.println("RX 95th percentile is " + rx95Percentile);
		System.out.println("TX 95th percentile is " + tx95Percentile);
		System.out.println("RX max is " + Long.toString(rxMax));
		System.out.println("TX max is " + Long.toString(txMax));
		System.out.println("RX average is " + (long)rxAverage);
		System.out.println("TX average is " + (long)txAverage);


		String rxTime = Long.toString((rxOver / (long)rxList.size())*100);
		String txTime = Long.toString((txOver / (long)txList.size())*100);

		//l's values correspond to: [rx-max, rx-mean, rx-95%, rx-time, tx-max, tx-mean, tx-95%, tx-time]

		l.add(new Value(Long.toString(rxMax), "rx", "max", measuredPeriod, circuit));
		l.add(new Value(Double.toString(rxAverage), "rx", "mean", measuredPeriod, circuit));
		l.add(new Value(Long.toString(rx95Percentile), "rx", "95%", measuredPeriod, circuit));
		l.add(new Value(rxTime, "rx", "time", measuredPeriod, circuit));
		l.add(new Value(Long.toString(txMax), "tx", "max", measuredPeriod, circuit));
		l.add(new Value(Double.toString(txAverage), "tx", "mean", measuredPeriod, circuit));
		l.add(new Value(Long.toString(tx95Percentile), "tx", "95%", measuredPeriod, circuit));
		l.add(new Value(txTime, "tx", "time", measuredPeriod, circuit));

		return l;
	}


	public static void main(String[] args) throws Exception {
		try{
			//Gets all properties in config.properties
			PropertyValues properties = new PropertyValues();
			final File path = new File(properties.directory);

			//Reads each file set in the directory string in config.properties and generates an ExcelForm object for each circuit
			ArrayList<ExcelForm> circuitArray = new ArrayList<ExcelForm>();
			for (File file : path.listFiles()) {
				System.out.println("\nReading files in: " + file.getAbsolutePath());
				for (File f : file.listFiles()) {

					String circuitID = null;

					String measuredPeriod = null;

					if (f.isFile() && f.getAbsolutePath().contains("csv")) {
						System.out.println("Reading file: " + f.getAbsolutePath());


						circuitID = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("\\") +1, f.getAbsolutePath().indexOf("_")).trim();
						measuredPeriod = f.getAbsolutePath().substring(f.getAbsolutePath().indexOf("_")+1, f.getAbsolutePath().indexOf("."));



						//Creates a new object if one with the same circuit does not already exist
						if(!ContainsID(circuitArray, circuitID)){
							circuitArray.add(new ExcelForm(circuitID, properties.directory, properties.bandwidth_profile, properties.bar_chart_template));
						}

						List <Value> valueList = CSVAnalyze(f, (long)circuitArray.get(circuitArray.size() - 1).billedBandwidth*1000000,
								measuredPeriod, circuitID, properties.generate_list);
						for (Value v : valueList)
						{
							circuitArray.get(circuitArray.size() - 1).LineInsert(v);
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

