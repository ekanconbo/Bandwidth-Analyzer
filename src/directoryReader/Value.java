package directoryReader;

/**
 * @author Justin Ross
 */

public class Value {
	String value; //Data value
	String bandwidthType; // TX or RX
	String valueType; //Max, mean, 95%, time
	String interval; // 24-7, M-F, M-F_8-5
	String circuitID; //LHNO-1234-LSUF, SUBM-2330-LSUD, etc.
	String identifier = bandwidthType + valueType + circuitID + interval; // Unique value created from each meta. EX: TXmaxLHNO-1234-LSUF24-7
	int profilePosition = 0; //0-23. Depending on where it is supposed to be placed in the Bandwidth Profile
	int barchartPositionx = 0;
	int barchartPositiony = 0;

	public Value(String value, String bandwidthType, String valueType, String interval, String circuitID){
		this.value = value;
		this.bandwidthType = bandwidthType;
		this.valueType = valueType;
		this.interval = interval;
		this.circuitID = circuitID;
		this.identifier = bandwidthType + valueType + circuitID + interval;
		GeneratePosition(bandwidthType, valueType);
		GenerateBarchartPosition(bandwidthType, valueType);

	}

	public Value(){
		this.value = null;
		this.bandwidthType = null;
		this.valueType = null;
		this.interval = null;
		this.circuitID = null;
		this.identifier = null;
		this.profilePosition = 0;
	}

	private void GeneratePosition(String bandwidthType, String valueType){
		if(bandwidthType.equals("tx"))
			profilePosition += 4;

		if(valueType.equals("95%"))
			profilePosition += 1;
		else if(valueType.equals("mean"))
			profilePosition += 2;
		else if(valueType.equals("time"))
			profilePosition += 3;

		if(interval.equals("M-F"))
			profilePosition += 8;
		else if(interval.equals("M-F_8-5"))
			profilePosition += 16;

	}

	private void GenerateBarchartPosition(String bandwidthType, String valueType){
		if(bandwidthType.equals("rx"))
			barchartPositionx += 1;

		if(valueType.equals("95%"))
			barchartPositiony += 1;
		else if(valueType.equals("mean"))
			barchartPositiony += 2;
		else if(valueType.equals("time"))
			barchartPositiony += 3;

		if(interval.equals("M-F"))
			barchartPositionx += 2;
		else if(interval.equals("M-F_8-5"))
			barchartPositionx += 4;
	}
}
