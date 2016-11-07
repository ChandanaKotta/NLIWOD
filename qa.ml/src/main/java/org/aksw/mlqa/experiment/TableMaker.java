package org.aksw.mlqa.experiment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import meka.classifiers.multilabel.BCC;
import meka.classifiers.multilabel.BPNN;
import meka.classifiers.multilabel.BRq;
import meka.classifiers.multilabel.CC;
import meka.classifiers.multilabel.CDN;
import meka.classifiers.multilabel.CDT;
import meka.classifiers.multilabel.CT;
import meka.classifiers.multilabel.DBPNN;
import meka.classifiers.multilabel.FW;
import meka.classifiers.multilabel.HASEL;
import meka.classifiers.multilabel.LC;
import meka.classifiers.multilabel.MCC;
import meka.classifiers.multilabel.MULAN;
import meka.classifiers.multilabel.MajorityLabelset;
import meka.classifiers.multilabel.MultiLabelClassifier;
import meka.classifiers.multilabel.PCC;
import meka.classifiers.multilabel.PMCC;
import meka.classifiers.multilabel.PS;
import meka.classifiers.multilabel.PSt;
import meka.classifiers.multilabel.RAkEL;
import meka.classifiers.multilabel.RAkELd;
import meka.classifiers.multilabel.RT;
import weka.core.Debug.Random;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

/*
 * This Class creates the Large Table for the Paper.
 */

public class TableMaker {
	static Logger log = LoggerFactory.getLogger(TableMaker.class);
	
	
	public static void main(String[] args) throws Exception {				 
		Path datapath= Paths.get("./src/main/resources/Qald6Logs.arff");
		BufferedReader reader = new BufferedReader(new FileReader(datapath.toString()));
		ArffReader arff = new ArffReader(reader);
		Instances data = arff.getData();
		data.setClassIndex(6);
		
		//Change To Classifier of Choice
		PSt Classifier = new PSt();
		Classifier.buildClassifier(data);

		
		JSONObject qald6test = Utils.loadTestQuestions();
			JSONArray questions = (JSONArray) qald6test.get("questions");
			ArrayList<String> testQuestions = Lists.newArrayList();
			for(int i = 0; i < questions.size(); i++){
				JSONObject questionData = (JSONObject) questions.get(i);
				JSONArray questionStrings = (JSONArray) questionData.get("question");
				JSONObject questionEnglish = (JSONObject) questionStrings.get(0);
				testQuestions.add((String) questionEnglish.get("string"));
			}
		ArrayList<String> systems = Lists.newArrayList("KWGAnswer", "NbFramework", "PersianQA", "SemGraphQA", "UIQA_withoutManualEntries", "UTQA_English" );
		double avef = 0;
		double[] systemavef = {0,0,0,0,0,0,0};
		for(int i=0; i<data.size(); i++){
			String tmp = "";
			tmp += i +"\t &" + testQuestions.get(i);
			double bestf = 0;
			for(String system: systems){
				double p = Float.parseFloat(Utils.loadSystemP(system).get(i));				
				double r = Float.parseFloat(Utils.loadSystemR(system).get(i));
				double f = 0;
				if(!(p==0&&r==0)){
					f = 2*p*r/(p+r);
				}
				if(f > bestf){
					bestf = f;
				}
				tmp += "\t &" + Math.floor(f * 100) / 100;
				systemavef[systems.indexOf(system)] += f/data.size();
			}
			systemavef[6] += bestf/data.size();
			tmp += "\t &" + Math.floor(bestf * 100) / 100;
			double[] confidences = Classifier.distributionForInstance(data.get(i));
			int argmax = -1;
			double max = -1;
				for(int j = 0; j < 6; j++){
					if(confidences[j]>max){
						max = confidences[j];
						argmax = j;
					}
				}
				
			String sys2ask = systems.get(systems.size() - argmax -1);
			double systemp = Float.parseFloat(Utils.loadSystemP(sys2ask).get(i));				
			double systemr = Float.parseFloat(Utils.loadSystemR(sys2ask).get(i));
			double systemf = 0;
			if(!(systemp==0&&systemr==0)){
				systemf = 2*systemp*systemr/(systemp+systemr);
			}
			avef += systemf;
			tmp += "\t &" + Math.floor(systemf * 100) / 100;

			tmp += "\\\\";
			System.out.println(tmp);
		}
		System.out.println(Arrays.toString(systemavef));
		System.out.println(avef/data.size());
	}
}