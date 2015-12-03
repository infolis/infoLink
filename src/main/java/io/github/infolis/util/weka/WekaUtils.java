package io.github.infolis.util.weka;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaUtils {

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
    public static Instances getInstances(Instances data, String classVal) {
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
        @SuppressWarnings("unchecked")
        Enumeration<Instance> instanceEnum = data.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());
            if (curClassVal.equals(new String(classVal))) {
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
    public static List<List<String>> getStrings(Instances data) {
        String studySubstitute = "<STUDYNAME> ";
        List<String> sentences_pos = new ArrayList<String>();
        List<String> sentences_neg = new ArrayList<String>();
        List<String> sentences;
        @SuppressWarnings("unchecked")
        Enumeration<Instance> instanceEnum = data.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());

            @SuppressWarnings("unchecked")
            Enumeration<Attribute> attributeEnum = data.enumerateAttributes();
            String contextString = "";
            if (curClassVal.equals(new String("True"))) {
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
        List<List<String>> resList = new ArrayList<List<String>>();
        resList.add(sentences_pos);
        resList.add(sentences_neg);
        return resList;
    }

    private static Instances getInstances(String arffFilename) throws FileNotFoundException, IOException {
        Reader reader = new InputStreamReader(new FileInputStream(arffFilename), "UTF-8");
        Instances data = new Instances(reader);
        reader.close();
        // setting class attribute
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }


}
