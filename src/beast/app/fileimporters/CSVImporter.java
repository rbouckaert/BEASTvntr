/*
* File CSVImporter.java
*
* Copyright (C) 2016 Arjun Dhawan, RIVM <arjun.dhawan@rivm.nl>
*
* This file is part of BEASTvntr.
*
* BEASTvntr is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* BEASTvntr is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with BEASTvntr.  If not, see <http://www.gnu.org/licenses/>.
*/

package beast.app.fileimporters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.*;



import beast.core.BEASTInterface;
import beast.app.beauti.AlignmentImporter;

import beast.evolution.datatype.FiniteIntegerData;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;


public class CSVImporter implements AlignmentImporter {
	public class DatatypeSelector { //just a class to ask the user whether to
		//import nucleotide or repeats, and to set the bounds for repeats
		String selectedDatatype;
		int minRepeat;
		int maxRepeat;
		FiniteIntegerData type = new FiniteIntegerData();

		public DatatypeSelector() {
			String[] selectionsDatatype = {"Repeats", "Nucleotides"};
			String datatype;
			selectedDatatype = (String) JOptionPane.showInputDialog(
				null,
				"Choose DataType (default: Repeats)",
				"Choose DataType",
				JOptionPane.PLAIN_MESSAGE,
				null,
				selectionsDatatype,
				selectionsDatatype[0]
			);
			
			switch (selectedDatatype) {
				case "Repeats":
					
					datatype = "finiteinteger";

		      JTextField minRepeatField = new JTextField(5);
		      JTextField maxRepeatField = new JTextField(5);

		      JPanel myPanel = new JPanel();
		      myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.PAGE_AXIS));

		      myPanel.add(new JLabel("<html>Choose the Minimum and Maximum repeat<br>(Maximum repeat - Minimum repeat >= 0 must hold)</html>"));
		      myPanel.add(new JLabel("Minimum repeat ( >= 0 ):"));
		      myPanel.add(minRepeatField);
		      myPanel.add(new JLabel("Maximum repeat:"));
		      myPanel.add(maxRepeatField);

		      int result = JOptionPane.showConfirmDialog(null, myPanel, 
		               "Please Enter Minimum and Maximum Repeat", JOptionPane.OK_CANCEL_OPTION);
		      if (result == JOptionPane.OK_OPTION) {
		      	minRepeat = Integer.parseInt(minRepeatField.getText());
		        maxRepeat = Integer.parseInt(maxRepeatField.getText());
		      	if (maxRepeat >= minRepeat && minRepeat >= 0) {		
					    type.setInputValue("minRepeat", minRepeat);
					    type.setInputValue("maxRepeat", maxRepeat);
					  	type.initAndValidate();
					  } else {
					  	throw new IllegalArgumentException("Bad values for maxRepeat: " + maxRepeat + ", minRepeat: " + minRepeat);
						}
					} else {
						throw new IllegalArgumentException("Minimum and maximum repeat were not specified");
					}
					break;
				case "Nucleotides":
					datatype = "nucleotide";
					break;
				default:
					throw new IllegalArgumentException("No datatype specified");
			}
		}

		public boolean checkParsedAllel(int parsedAllel) {//check for non sensical values
			switch (selectedDatatype) {
				case "Repeats":
				// Remember that -1 means ambiguous
					if ((parsedAllel > 0 && parsedAllel < minRepeat) || parsedAllel > maxRepeat) {
						return true;
					} else {
						return false;
					}
				case "Nucleotides":
					return false;
				default:
					return false;
			}
		}
	}

	public String [] getFileExtensions() {
		return new String[]{"csv"};
	}

	public List<BEASTInterface> loadFile(File file) {    
		try {
		// start reading the csv file
		BufferedReader fin = new BufferedReader(new FileReader(file));

		DatatypeSelector datatypeSelection = new DatatypeSelector();

    Alignment m_alignment = new Alignment();

    while (fin.ready()) {
    	String line = fin.readLine();
    	if ( line.trim().length() == 0 ) {
					continue;  // Skip blank lines
			}
    	String[] row = line.split(",");
    	String currentTaxon = row[0];

			if (currentTaxon == null || currentTaxon.trim().length() == 0) {
				// check if not null and not only white space
				fin.close();
				throw new IllegalArgumentException("Expected taxon defined on first line");
			}
    	StringBuilder sb = new StringBuilder();
    	int rowLength = row.length;
    	
    	for (int i = 1; i < row.length; i++) {
    		switch (datatypeSelection.selectedDatatype) {
					case "Repeats":
    				int parsedAllel = Integer.parseInt(row[i]);
    				if (datatypeSelection.checkParsedAllel(parsedAllel)) {
							throw new IllegalArgumentException("Encountered repeat out of bounds: " + parsedAllel);
						}
    				sb.append(parsedAllel);
    				sb.append(",");
    				break;
    			case "Nucleotides":
    				sb.append(row[i]);
    		}
    	}
    	String sequenceData = sb.toString();
    	Sequence sequence = new Sequence();
    	
    	if (datatypeSelection.selectedDatatype == "Repeats") {
    		sequence.init(datatypeSelection.maxRepeat - datatypeSelection.minRepeat + 1, currentTaxon, sequenceData);
    	} else if (datatypeSelection.selectedDatatype == "Nucleotides") {
    		sequence.init(4, currentTaxon, sequenceData);
    	}
    	sequence.setID("seq_" + currentTaxon);
    	m_alignment.sequenceInput.setValue(sequence, m_alignment);
    }
    fin.close();
    
    String ID = file.getName();
    ID = ID.substring(0, ID.lastIndexOf('.')).replaceAll("\\..*", "");
    m_alignment.setID(ID);

    if (datatypeSelection.selectedDatatype == "Repeats") {
    	m_alignment.setInputValue("userDataType", datatypeSelection.type);
    } else if (datatypeSelection.selectedDatatype == "Nucleotides") {
    	m_alignment.dataTypeInput.setValue("nucleotide", m_alignment);
    }

    m_alignment.initAndValidate();
    return Arrays.asList(m_alignment);
	} catch (Exception e) {
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + e.getMessage());
		return null;
		}
	}
}
