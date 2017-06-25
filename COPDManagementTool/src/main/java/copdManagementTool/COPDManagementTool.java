package copdManagementTool;

import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

//import javax.script.ScriptEngine;
//import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import java.io.FileInputStream; 
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet; 
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.github.rcaller.datatypes.DataFrame;
import com.github.rcaller.rstuff.RCaller;
//import com.github.rcaller.util.Globals;
import com.github.rcaller.rstuff.RCode;
//import com.github.rcaller.scriptengine.RCallerScriptEngine;

public class COPDManagementTool {

	private File selectedDB;
	private List<ParameterConfiguration> parameterConfigurations = new ArrayList<ParameterConfiguration>();
	private JComponent paramConfigPanel;

	private void updateDBLabel(JLabel selectedDBLabel){
		if (selectedDB == null) {
			selectedDBLabel.setText("No DB is loaded");
		} else {
			selectedDBLabel.setText("Loaded DB: "+selectedDB.getName());
		}	
	}

	public C50Inputs getC50Inputs() throws IOException {
		
		C50Inputs c50Inputs = new C50Inputs();
		XSSFSheet sheet = getExcelSheet(selectedDB);
		int rowNum = sheet.getLastRowNum()+1;
		//System.out.println(rowNum);
		int colNum = parameterConfigurations.size();
		int i=0, j = 0,k = 0;
		
		c50Inputs.inputValues = new Object[countIncludedInputs()][rowNum-1];
		c50Inputs.targetValues = new Object[1][rowNum-1];
		c50Inputs.inputNames = new String[countIncludedInputs()];
		c50Inputs.targetNames = new String[1];
		for (i=0; i< colNum;i++) {

			if(parameterConfigurations.get(i).getIsIncluded().isSelected() == true){

				if(parameterConfigurations.get(i).getRole().getSelectedItem() == "Input"){

					for(j=0;j<rowNum;j++){

						if(j!=0){
							Cell currentCell = sheet.getRow(j).getCell(i,MissingCellPolicy.CREATE_NULL_AS_BLANK);
							c50Inputs.inputValues[k][j-1] = getCellValue(currentCell);
						}else{
							c50Inputs.inputNames[k] = sheet.getRow(j).getCell(i).getStringCellValue();
						}
					}
					k++;	
				}else{
					
					for(j=0;j<rowNum;j++){
						
						if(j!=0){
							Cell currentCell = sheet.getRow(j).getCell(i,MissingCellPolicy.CREATE_NULL_AS_BLANK);
							c50Inputs.targetValues[0][j-1] = getCellValue(currentCell);
						}else{
							c50Inputs.targetNames[0] = sheet.getRow(j).getCell(i).getStringCellValue();
						}
					}
				}
			}
		}
		return c50Inputs;
	}

	public JComponent  makeTrainModelPanel(){

		JPanel trainModelPanel = new JPanel();
		trainModelPanel.setLayout(new BoxLayout(trainModelPanel, BoxLayout.Y_AXIS));
		Container line = new Container();
		line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
		JLabel selectedDBLabel = new JLabel();
		line.add(selectedDBLabel);
		updateDBLabel(selectedDBLabel);
		JFileChooser fileChooser = new JFileChooser();
		JButton fileChooserButton = new JButton("Load DB");
		fileChooserButton.addActionListener(e -> fileChooser.showOpenDialog(trainModelPanel));
		line.add(fileChooserButton);								
		trainModelPanel.add(line);
		fileChooser.addActionListener((ev) -> {
			selectedDB = fileChooser.getSelectedFile();
			if (selectedDB!=null) {
				System.out.println("file selezionato "+selectedDB);

				if (paramConfigPanel!=null) {
					trainModelPanel.remove(paramConfigPanel);
					parameterConfigurations.clear();
					
				}
				try {
					paramConfigPanel = makeParamConfigPanel(selectedDB);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				trainModelPanel.add(paramConfigPanel);
				trainModelPanel.revalidate();
			}
			updateDBLabel(selectedDBLabel);
			
		});

		return trainModelPanel;
	}

	public JComponent makeParamConfigPanel(File selectedFile) throws IOException {
		JPanel paramConfigPanel = new JPanel();
		paramConfigPanel.setLayout(new BoxLayout(paramConfigPanel, BoxLayout.Y_AXIS));


		XSSFSheet sheet = getExcelSheet(selectedFile);
		String[] paramNames = readParamNames(sheet);
		
		for (int i =0; i<paramNames.length; i++) {

			ParameterConfiguration paramConfig = new ParameterConfiguration(paramNames[i]);
			this.parameterConfigurations.add(paramConfig);

			Container riga = Box.createHorizontalBox();
			riga.add(new JLabel(paramConfig.getParamName()));
			riga.add(paramConfig.getIsIncluded());
			riga.add(paramConfig.getType());
			riga.add(paramConfig.getRole());

			paramConfigPanel.add(riga);
		}
		JButton trainModelButton = new JButton("Train Model");
		trainModelButton.addActionListener((ae)->{
			try {
				trainModel();
			} catch (IOException | ScriptException e) {
				e.printStackTrace();
			}
		});
		paramConfigPanel.add(trainModelButton);
		return paramConfigPanel;
	}

	public static class ParameterConfiguration {            

		private String paramName;
		private JCheckBox isIncluded;
		private JComboBox<String> paramType;
		private JComboBox<String> paramRole;

		public ParameterConfiguration(String paramName) {
			super();
			this.paramName = paramName;
			this.isIncluded = new JCheckBox();
			this.isIncluded.setSelected(true);
			this.paramType = new JComboBox<>(new String[]{"Numeric","Categoric"});
			this.paramRole = new JComboBox<>(new String[]{"Input","Target"});
		}

		public String getParamName() {
			return paramName;
		}

		public JCheckBox getIsIncluded() {
			return isIncluded;
		}

		public JComboBox<String> getType() {
			return paramType;
		}

		public JComboBox<String> getRole() {
			return paramRole;
		}

	}

	public void Start(){

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		JFrame mainFrame = new JFrame();
		mainFrame.setSize(800, 600);
		mainFrame.setResizable(true);
		mainFrame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setTitle("COPD Management Tool");
		JTabbedPane tabbedPanel = new JTabbedPane();
		JComponent trainModelPanel = makeTrainModelPanel();
		tabbedPanel.addTab("Train Model", trainModelPanel);
		JComponent predictPanel= makePredictPanel();
		tabbedPanel.add("Predict", predictPanel);
		mainFrame.add(tabbedPanel);
		mainFrame.setVisible(true);
	}


	public JComponent makePredictPanel(){
		JPanel PredictPanel = new JPanel();

		return PredictPanel;
	}



	public static void main(String[] args) {
		COPDManagementTool newCOPDManagementTool = new COPDManagementTool();
		newCOPDManagementTool.Start();
	}

	public XSSFSheet getExcelSheet (File selectedFile) throws IOException{
		
		FileInputStream fis = new FileInputStream(selectedFile);
		XSSFWorkbook book = new XSSFWorkbook(fis);
		XSSFSheet sheet = book.getSheetAt(0);
		book.close();
		return sheet;
		
	}
	
	public String[] readParamNames(XSSFSheet sheet){
		
		int paramNum = sheet.getRow(0).getLastCellNum();
		String[] paramNames = new String[paramNum];
		XSSFRow firstRow = sheet.getRow(0);
		for(int i=0; i<paramNum;i++){
			paramNames[i] = firstRow.getCell(i).getStringCellValue();
		}
			
		return paramNames;
		
	}
		

	public class C50Inputs{
		String[] inputNames;
		Object[][] inputValues;
		String[] targetNames;
		Object[][] targetValues;
	}
	
	public void trainModel() throws IOException, ScriptException{
		
		C50Inputs inputs = getC50Inputs();
		
		for(int i=0;i<10;i++){
			System.out.println(inputs.targetNames[0]);
			System.out.println(inputs.targetValues[0][i]);
		}
		
		
		DataFrame inputsDataframe = DataFrame.create(inputs.inputValues,inputs.inputNames);
		//DataFrame targetDataframe = DataFrame.create(inputs.targetValues,inputs.targetNames);
		RCaller rCaller = RCaller.create();
		RCode rCode = RCode.create();
		//rCode.R_require("C50");
		rCode.addDataFrame("Inputs", inputsDataframe);
		convertToFactor(rCode,inputs.inputNames,"Inputs");
		//rCode.addDataFrame("Target",targetDataframe);
		//convertToFactor(rCode,inputs.targetNames,"Target");
		//rCode.addRCode("CostMatrix = matrix(c(0,1,1,1,0,1,1,1,0),nrow=3,ncol=3,dimnames =list(c('1','2','3'),c('1','2','3'))))");
		//rCode.addRCode("Control = C5.0Control(CF=0.8,minCases=1)");
		//rCode.addRCode("TrainedModel = C5.0(Predictors,Target,control = Control, costs = CostMatrix)");
		rCode.addRCode("saveRDS(Inputs,file='TrainedModel.rds')");
		rCaller.setRCode(rCode);
		rCaller.runOnly();	           
	}

	public int countIncludedInputs(){
		
		int count = 0;
		for(int i=0; i< parameterConfigurations.size();i++){
			
			if(parameterConfigurations.get(i).getIsIncluded().isSelected() == true && 
					parameterConfigurations.get(i).getRole().getSelectedItem() == "Input" ){
				count++;
			}
		}
		 
		return count;
	}
	
	public void convertToFactor(RCode rCode, String[] paramList, String dataFrame){
		
		
		for(int i=0; i< paramList.length;i++){
			int j=0;
			while(paramList[i].equals(parameterConfigurations.get(j).getParamName())==false){
				j++;
			}
			if(parameterConfigurations.get(j).getType().getSelectedItem() == "Categoric"){
				rCode.addInt("i", i);
				rCode.addRCode(dataFrame+"[,i+1] = factor("+dataFrame+"[,i+1])");
		}

	}
	}
		
	public Object getCellValue(Cell currentCell){
		
		switch(currentCell.getCellTypeEnum()){
		case NUMERIC: return currentCell.getNumericCellValue(); 
		case STRING:  return currentCell.getStringCellValue();  
		case BOOLEAN: return currentCell.getBooleanCellValue();
		case BLANK: return "NA";
		default: return "NA";
		
		
		}
	}

}
