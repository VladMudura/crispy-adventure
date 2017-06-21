package copdManagementTool;
import java.awt.Container;
//import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import java.io.FileNotFoundException; 
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet; 
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


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

	public void addestraModello() {
		for (ParameterConfiguration cParam: this.parameterConfigurations) {
			System.out.println("Colonna "+cParam.paramName+" incluso: "+cParam.getIsIncluded().isSelected()+
					" tipo: "+cParam.getType().getSelectedItem()+" ruolo: "+cParam.getRole().getSelectedItem());
		}
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
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				trainModelPanel.add(paramConfigPanel);	
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
			addestraModello();
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
		
	public ExcelWorkbook readExcel(){

		ExcelWorkbook selectedFile = new ExcelWorkbook();

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(selectedDB);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XSSFWorkbook book = null;
		try {
			book = new XSSFWorkbook(fis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		XSSFSheet sheet = book.getSheetAt(0);
		selectedFile.paramNum = sheet.getRow(0).getLastCellNum();
		selectedFile.rowNum = sheet.getLastRowNum();

		selectedFile.paramNames = new String[selectedFile.paramNum];
		selectedFile.paramValues = new Object[selectedFile.paramNum][selectedFile.rowNum-1];


		for(int j=0; j<selectedFile.rowNum;j++){
			for(int i=0; i<selectedFile.paramNum;i++){
				Cell currentCell = sheet.getRow(j).getCell(i);
				if(j==0){
					selectedFile.paramNames[i] = currentCell.getStringCellValue();
				}
				else{

					if (currentCell.getCellTypeEnum() == CellType.STRING) {
						selectedFile.paramValues[i][j-1] = currentCell.getStringCellValue();

					} else if (currentCell.getCellTypeEnum() == CellType.NUMERIC) {
						selectedFile.paramValues[i][j-1] = currentCell.getNumericCellValue();
					}

				}
			}

		}
		return selectedFile;


	}

	public class ExcelWorkbook{
		String[] paramNames;
		Object[][] paramValues;
		int paramNum;
		int rowNum;
	}



}
