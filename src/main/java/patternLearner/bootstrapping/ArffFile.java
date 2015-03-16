/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 *
 * @author domi
 */
/**
 * Class for representing training instances in weka's arff-file format. Each
 * training instance is assumed to consist of 10 string attributes representing
 * the surrounding words of a term + one class value for the binary relation
 * <emph>isStudyReference</emph>. Class values may either be
 * <emph>"True"</emph> (positive training examples) or <emph>"False"</emph>
 * (negative training examples).
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class ArffFile {

    private Instances data = null;
    private String fileName;

    /**
     * Class constructor specifying the Instances to represent.
     *
     * @param data	the Instances to represent
     */
    public ArffFile(Instances data) {
        this.data = data;
    }

    public ArffFile(String fileName) {
        this.fileName = fileName;
        this.data = getInstances();
        System.out.println(this.data);
    }

    /**
     * Class constructor specifying a set of values to construct the training
     * instances to represent.
     *
     * @param instance	values for constructing the Instances to represent. Each
     * instance is assumed to consist of 10 string attributes representing the
     * surrounding words of a term + one class value for the binary relation
     * <emph>isStudyReference</emph>. Class values may either be
     * <emph>"True"</emph> (positive training examples) or
     * <emph>"False"</emph> (negative training examples).
     */
    public ArffFile(Set<String[]> instances, String fileName) {
        this.fileName = fileName;
        FastVector atts;
        FastVector attVals;
        double[] vals;

        // 1. set up attributes
        atts = new FastVector();

        // - string
        atts.addElement(new Attribute("l5", (FastVector) null));
        atts.addElement(new Attribute("l4", (FastVector) null));
        atts.addElement(new Attribute("l3", (FastVector) null));
        atts.addElement(new Attribute("l2", (FastVector) null));
        atts.addElement(new Attribute("l1", (FastVector) null));
        atts.addElement(new Attribute("r1", (FastVector) null));
        atts.addElement(new Attribute("r2", (FastVector) null));
        atts.addElement(new Attribute("r3", (FastVector) null));
        atts.addElement(new Attribute("r4", (FastVector) null));
        atts.addElement(new Attribute("r5", (FastVector) null));

        // - nominal class attribute
        attVals = new FastVector();
        attVals.addElement("True");
        attVals.addElement("False");
        Attribute classAttr = new Attribute("class", attVals);
        atts.addElement(classAttr);

        // 2. create Instances object
        data = new Instances("IsStudyReference", atts, 0);
        data.setClass(classAttr);

        // 3. fill with data
        for (String[] neighboringWords : instances) {
            vals = new double[data.numAttributes()];
            // - string
            vals[0] = data.attribute(0).addStringValue(neighboringWords[0]);
            vals[1] = data.attribute(1).addStringValue(neighboringWords[1]);
            vals[2] = data.attribute(2).addStringValue(neighboringWords[2]);
            vals[3] = data.attribute(3).addStringValue(neighboringWords[3]);
            vals[4] = data.attribute(4).addStringValue(neighboringWords[4]);
            vals[5] = data.attribute(5).addStringValue(neighboringWords[5]);
            vals[6] = data.attribute(6).addStringValue(neighboringWords[6]);
            vals[7] = data.attribute(7).addStringValue(neighboringWords[7]);
            vals[8] = data.attribute(8).addStringValue(neighboringWords[8]);
            vals[9] = data.attribute(9).addStringValue(neighboringWords[9]);

            // add class value (nominal)
            vals[10] = attVals.indexOf(neighboringWords[10]);

            Instance newInstance = new Instance(1.0, vals);

            // add instance
            newInstance.setDataset(data);
            data.add(newInstance);
        }
    }

    /**
     * Calls the saveToFile method to write this Instance data to file,
     * additionally prints a summary of the data and does the error handling.
     *
     * @param filename	name of the output file
     */
    public void write() {
        try {
            saveToFile();
            System.out.println(this.getData().toSummaryString());
            System.out.println("Wrote " + this.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(getData());
        }
    }

    /**
     * Writes this Instance data to file.
     *
     * @param filename	name of the output file
     * @throws IOException
     */
    public void saveToFile() throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(this.getData());
        saver.setFile(new File(this.getFileName()));
        saver.writeBatch();
    }

    private Instances getInstances() {
        if(this.getData()== null) {
            try {
                Reader reader = new InputStreamReader(new FileInputStream(this.getFileName()), "UTF-8");
                this.data = new Instances(reader);                
            } catch(Exception e ){
                e.printStackTrace();
            }    
        // setting class attribute
            getData().setClassIndex(getData().numAttributes() - 1);
        }
        return getData();
    }
    
    /**
     * Retrieves all training instances from the specified data having the
     * specified class attribute. Note: each instance is required to have
     * exactly 10 attributes representing 5 words before and 5 words after the
     * dataset name + one class attribute. Instances having class attribute
     * <emph>True</emph> are positive examples for the relation
     * <emph>IsStudyReference</emph>, instances having class attribute
     * <emph>False</emph> are negative examples.
     *
     * @param data	the training examples to learn from
     * @return	Instances having class <emph>classVal</emph>
     */
    public Instances getInstancesByClassAttribute(String classVal) {
        FastVector atts = new FastVector();
        FastVector attVals;
        atts.addElement(new Attribute("l5", (FastVector) null));
        atts.addElement(new Attribute("l4", (FastVector) null));
        atts.addElement(new Attribute("l3", (FastVector) null));
        atts.addElement(new Attribute("l2", (FastVector) null));
        atts.addElement(new Attribute("l1", (FastVector) null));
        atts.addElement(new Attribute("r1", (FastVector) null));
        atts.addElement(new Attribute("r2", (FastVector) null));
        atts.addElement(new Attribute("r3", (FastVector) null));
        atts.addElement(new Attribute("r4", (FastVector) null));
        atts.addElement(new Attribute("r5", (FastVector) null));
        attVals = new FastVector();
        attVals.addElement(classVal);
        Attribute classAttr = new Attribute("class", attVals);
        atts.addElement(classAttr);
        Instances data_matchingClass = new Instances("IsStudyReference_" + classVal, atts, 0);
        data_matchingClass.setClass(classAttr);

        // iterate over instances, check value of class attribute
        // return only instances with classVal: disregard instances with other class
        Enumeration<Instance> instanceEnum = getData().enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());
            if (curClassVal.equals(classVal)) {
                Instance newInstance = new Instance(11);
                newInstance.setDataset(data_matchingClass);
                // loop over all attributes and fill in values
                // copying values from an existing instance using 
                // Instance newInstance = new Instance(curInstance);
                // does not work...
                for (int i = 0; i < 11; i++) {
                    newInstance.setValue(i, curInstance.stringValue(i));
                }
                data_matchingClass.add(newInstance);
            }
        }
        return data_matchingClass;
    }
    
    /**
     * Returns the attributes of the instances in data as strings
     *
     * @param data	the training examples
     * @return	first list containing all sentences of positive training
     * instances, second list containing all sentences of negative training
     * instances
     */
    public List<String>[] getStrings(Instances data) {
        String studySubstitute = "<STUDYNAME> ";
        List<String> sentences_pos = new ArrayList();
        List<String> sentences_neg = new ArrayList();
        List<String> sentences;
        Enumeration<Instance> instanceEnum = data.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());

            Enumeration<Attribute> attributeEnum = data.enumerateAttributes();
            String contextString = "";
            if (curClassVal.equals("True")) {
                sentences = sentences_pos;
            } else {
                sentences = sentences_neg;
            }

            while (attributeEnum.hasMoreElements()) {
                Attribute curAtt = attributeEnum.nextElement();
                String attVal = curInstance.stringValue(curAtt);
                contextString += attVal + " ";
                if (curAtt.index() == 4) {
                    contextString += studySubstitute;
                }
            }
            sentences.add(contextString);
        }
        List<String>[] resList = new ArrayList[2];
        resList[0] = sentences_pos;
        resList[1] = sentences_neg;
        return resList;
    }

    /**
     * @return the data
     */
    public Instances getData() {
        return data;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }
}
