package directoryReader;

import java.io.*;
import java.util.*;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * @author Justin Ross
 */

public class ExcelForm {
	String title = null;
	String circuitNum = null; //1218, 1336, etc.

	String filePath = null;
	String bwProfilePath = null;
	String barChartTemplatePath = null;

	String customerName;


	double billedBandwidth; //in Mbps
	double circuitBandwidth; //in Mbps

	int excelRow;

	List<Value> bwValues = new ArrayList<Value>() ; //24 is the number of values to appear on a single line in the Excel Bandwidth Profile

	public ExcelForm(String circuitID, String filePath, String bwProfilePath, String barChartTemplatePath) throws EncryptedDocumentException, InvalidFormatException, IOException{
		this.title = circuitID;
		this.filePath = filePath; //Equal to String directory in DirectoryReader.java
		this.circuitNum = circuitID.substring(circuitID.indexOf("-")+1, circuitID.indexOf("-")+5);
		this.bwProfilePath = bwProfilePath;
		this.barChartTemplatePath = barChartTemplatePath;
		bwValues.addAll(Collections.nCopies(24, new Value())); //24 is the number of values on a single line in the Excel Bandwidth Profile

		InputStream input = new FileInputStream(bwProfilePath);

		Workbook wb = WorkbookFactory.create(input);
		Sheet sheet = wb.getSheetAt(0);
		int count = 0;
		for(Row row : sheet){
			if(row.getCell(7).getRichStringCellValue().getString().trim().equals(this.title)){
				this.excelRow = count;
				this.billedBandwidth = row.getCell(3).getNumericCellValue(); //Assumes Billed Bandwidth is column 4
				this.circuitBandwidth = row.getCell(4).getNumericCellValue(); //Assumes Circuit Bandwidth is column 5
				this.customerName = row.getCell(2).getStringCellValue(); //Assumes Customer is column 3
			}
			count++;
		}

	}



	public void LineInsert(Value v) throws Exception{
		if(bwValues.get(v.profilePosition).identifier == null){
			bwValues.set(v.profilePosition, v);
		}
		else
			throw new Exception("Attempting to write over value!"
					+ "\nIdentifier of value attempting to insert at this position: "+v.identifier
					+ "\nIdentifier at this position: "+bwValues.get(v.profilePosition).identifier+"\nExpected: null"
					+ "\nAttempting to insert value: "+v.value
					+ "\nValue already located in this position: "+bwValues.get(v.profilePosition).value);
	}




	//Creates a file to easily copy and paste into 2015 Participants Bandwidth Profile
	public void GenerateLine() throws Exception{
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath+"/"+title+"/"+title+"_Profile Data"+".txt"), "utf-8"))) {
			for(Value value : bwValues){
				if (value.profilePosition == 0){
					writer.write(value.value);
				}
				else{
					writer.write("\t"+value.value);
				}
			}
		} catch(IOException ex){
			ex.printStackTrace();
		}
	}

	//Writes relevant data to Bandwidth Profile.
	public void WriteLine() throws Exception{
		InputStream input = new FileInputStream(bwProfilePath);
		Workbook wb = WorkbookFactory.create(input);
		Sheet sheet = wb.getSheetAt(0);
		for(Value value : bwValues){
			if(excelRow != 0){
				if(!value.value.equals("N/A") && !value.valueType.equals("time")){
					sheet.getRow(excelRow).getCell(26+value.profilePosition).setCellValue(Double.parseDouble(value.value));
					sheet.getRow(excelRow).getCell(26+value.profilePosition).setCellType(Cell.CELL_TYPE_NUMERIC);
				}
				else if(!value.value.equals("N/A")){
					sheet.getRow(excelRow).getCell(26+value.profilePosition).setCellValue(Double.parseDouble(value.value)/100);
					sheet.getRow(excelRow).getCell(26+value.profilePosition).setCellType(Cell.CELL_TYPE_NUMERIC);
				}
				else
					sheet.getRow(excelRow).getCell(26+value.profilePosition).setCellValue(value.value);

			}
		}
		FileOutputStream fileOut = new FileOutputStream(bwProfilePath);
		wb.write(fileOut);
		fileOut.close();

	}


	//Creates Excel Bar Chart
	public void GenerateBarChart() throws Exception{
		InputStream input = new FileInputStream(barChartTemplatePath);
		Workbook wb = WorkbookFactory.create(input);
		Sheet sheet = wb.getSheetAt(0);
		CellStyle style = wb.createCellStyle();
		style.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
		for(Value v : bwValues){
			Row row = sheet.getRow(43+v.barchartPositiony);
			Cell cell = row.getCell(2+v.barchartPositionx);
			if(!v.value.equals("N/A") && !v.valueType.equals("time")){
				cell.setCellValue(((Double.parseDouble(v.value))/(float)1000000));
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			}
			else if(!v.value.equals("N/A")){
				cell.setCellValue(Double.parseDouble(v.value)/100);
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			}
			else
				cell.setCellValue(v.value);
		}
		Cell cell = sheet.getRow(38).getCell(4);
		cell.setCellValue(this.title);
		sheet.getRow(48).getCell(2).setCellType(Cell.CELL_TYPE_NUMERIC);
		sheet.getRow(48).getCell(2).setCellValue(billedBandwidth);
		sheet.getRow(50).getCell(2).setCellType(Cell.CELL_TYPE_NUMERIC);
		sheet.getRow(50).getCell(2).setCellValue(circuitBandwidth);
		sheet.getRow(39).getCell(2).setCellValue(this.customerName);
		FileOutputStream fileOut = new FileOutputStream(this.filePath+"/"+title+"/"+title+".xlsx");
		wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
		wb.write(fileOut);
		fileOut.close();

	}



}
