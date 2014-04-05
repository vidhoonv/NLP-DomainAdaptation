import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.PrintStream;
import java.io.OutputStream;

import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.NumberRangeFileFilter;
import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.ScoredObject;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.parser.lexparser.AbstractTreeExtractor;
import edu.stanford.nlp.parser.lexparser.BinaryGrammar;
import edu.stanford.nlp.parser.lexparser.BinaryGrammarExtractor;
import edu.stanford.nlp.parser.lexparser.DependencyGrammar;
import edu.stanford.nlp.parser.lexparser.EvalbFormatWriter;
import edu.stanford.nlp.parser.lexparser.EvaluateTreebank;
import edu.stanford.nlp.parser.lexparser.GrammarCompactor;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Lexicon;
import edu.stanford.nlp.parser.lexparser.LinearGrammarSmoother;
import edu.stanford.nlp.parser.lexparser.MLEDependencyGrammarExtractor;
import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.parser.lexparser.ParserQuery;
import edu.stanford.nlp.parser.lexparser.SplittingGrammarExtractor;
import edu.stanford.nlp.parser.lexparser.TreeAnnotatorAndBinarizer;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.parser.lexparser.UnaryGrammar;
import edu.stanford.nlp.parser.metrics.AbstractEval;
import edu.stanford.nlp.parser.metrics.BestOfTopKEval;
import edu.stanford.nlp.parser.metrics.Eval;
import edu.stanford.nlp.parser.metrics.Evalb;


public class myDaParser {

	/**
	 * @param args
	 */
	
	//data 
	Treebank wsj_02_22,wsj_23;
	MemoryTreebank  brown90Pc, brown10Pc;
	public LexicalizedParser lp = null,dalp = null;
	public Options op = null, daop=null;
	//testing
    public EvaluateTreebank evaluator = null;
	
	public int seedInstanceCounter, stInstanceCounter;
	
	//results
	public double outDomainF1, inDomainF1, domainAdaptedF1; 
	
	public MemoryTreebank seedDataTreebank;
	public MemoryTreebank outDomainTestbank;
	public MemoryTreebank inDomainDataTreebank;
	public MemoryTreebank selfTrainTreebank;
	public MemoryTreebank testTreebank ;
	public MemoryTreebank tempOutputTreebank;

	
	
	
	public  myDaParser() {
		op = new Options();
		daop = new Options();
		brown90Pc = new MemoryTreebank();
		brown10Pc = new MemoryTreebank();
		seedDataTreebank = new MemoryTreebank();
		outDomainTestbank = new MemoryTreebank();
		inDomainDataTreebank = new MemoryTreebank();
		selfTrainTreebank = new MemoryTreebank();
		testTreebank = new MemoryTreebank();
		tempOutputTreebank = new MemoryTreebank();
		seedInstanceCounter=0;
		stInstanceCounter=0;
		outDomainF1 = 0.0;
		inDomainF1 = 0.0;
		domainAdaptedF1 = 0.0;
		/*
		System.setErr(new PrintStream(new OutputStream() {
		    public void write(int b) {
		    }
		}));
	*/	
	}
	


   
	public  Treebank makeTreebank(String path, FileFilter filt) {
	    System.err.println("Creating treebank dir: " + path);
	    Treebank tTreebank = op.tlpParams.diskTreebank();
	    System.err.print("Reading trees...");	    
	    if (filt == null) {
		      tTreebank.loadPath(path);
		    } else {
		      tTreebank.loadPath(path, filt);
		    }
	    Timing.tick("done [read " + tTreebank.size() + " trees].");
	    return tTreebank;
	  }
	
	

	

	public void splitBrownFromDir(String path) {
		//file processing related
		File inDomainDirPath = new File(path);
		//create in domain data
		Treebank tempTreebank;
		for(File inputFile : inDomainDirPath.listFiles()) {
		
			  System.out.println("VIDH: Obtaining indomain data from file: "+inputFile);
			  tempTreebank = makeTreebank(inputFile.getAbsolutePath(),null);
			  
			  splitBrownTreebank(tempTreebank,90);

		  }
		//return tMainbank;
	}
	public void splitBrownTreebank(Treebank tdatabank,int splitProp){
	
			int bankSize = tdatabank.size();
			int sentenceCounter = 0;
			int trainCount=0,testCount=0;  
	      
			for (Tree treeInstance : tdatabank) {
			    
				if(sentenceCounter < bankSize*splitProp/100) {
					brown90Pc.add(treeInstance);
		        	trainCount++;
		        	
				}
				else {
					brown10Pc.add(treeInstance);
					testCount++;
		        	
				}
				sentenceCounter++;       
	      }
		  
		
		  	  
	      System.out.println("VIDH: train counter: "+trainCount+" bank size: "+brown90Pc.size());
	      System.out.println("VIDH: test counter: "+testCount+" bank size: "+brown10Pc.size());
	}
	public Options setParserOptions(){
		 Options op = new Options();
		 op.doDep = false;
		 op.doPCFG = true;
		 op.setOptions("-goodPCFG", "-evals", "tsv");
		 
		 return op;
	}
	
	public void trainParser(Options op){
		lp = LexicalizedParser.trainFromTreebank(seedDataTreebank, op);
	}
	
	public void setTestingOptions(String prefix, String ext){
		//op.testOptions.writeOutputFiles = true;
		op.testOptions.outputFilesPrefix = new String(prefix);
		op.testOptions.outputFilesExtension = new String(ext);
		op.testOptions.verbose = false;
		//op.testOptions.testingThreads = 10;
	}
	
	
	public  void getParseTreebankForSelfTraindata(){		  
		int counter=0;
	    for (Tree treeInstance : selfTrainTreebank) {
	    	
	        List<CoreLabel> sentence = getInputSentence(treeInstance,op);
	        Tree outputTree = lp.apply(sentence);
	        
	        //end of copy
	        Function<Tree, Tree> a = TreeFunctions.getLabeledTreeToStringLabeledTreeFunction();
	        outputTree = a.apply(outputTree);

	        seedDataTreebank.add(outputTree);
	        if(counter%1000 == 0) {
	    		System.out.println("VIDH: Self Training Progress: "+ counter+ " trees done.");
	    	//	System.out.println("tree instance !!!!!");
	    	//	treeInstance.pennPrint();
	    		//System.out.println("output instance !!!!!");
	    		//outputTree.pennPrint();
	    		//System.out.println("seedbank instance !!!!!");
	    		//seedDataTreebank.get(seedDataTreebank.size()-1).pennPrint();
	    	}
	        counter++;
	      }
	   	}
	
	public  List<CoreLabel> getInputSentence(Tree t,Options op) {
	      return Sentence.toCoreLabelList(t.yieldWords());
	}



	public void printResults(){
	System.out.println("RESULTS: outDomain-F1\tinDomain-F1\tdomainAdapted-F1");
	System.out.println("RESULTS: "+outDomainF1+"\t"+inDomainF1+"\t"+domainAdaptedF1);
	}

	/*

	public void createEvaluator(){
		 evaluator = new EvaluateTreebank(lp);
	}

	public double evaluate(Treebank tTreebank){
		System.out.println("check: "+tTreebank.size());
		return(evaluator.testOnTreebank(tTreebank));		
	}
*/
	/*
    public void mergeSelfTrainingResults(){
    	
    	 for (Tree treeInstance : tempOutputTreebank) {
    		
    		 seedDataTreebank.add(treeInstance);
		  }
    }

    
}
	   */ 
	
	public MemoryTreebank reduceTreebank(Treebank currentTreebank,int newSize){
		
		int counter =0;
		MemoryTreebank newTreebank = new MemoryTreebank();
		for(Tree treeInstance : currentTreebank){
			if(counter<newSize) {
				newTreebank.add(treeInstance);				
			}
			else{
				break;
			}
			counter++;
		}
		return newTreebank;
		
	}

		public MemoryTreebank convertToMembank(Treebank tBank) {
			MemoryTreebank newTreebank = new MemoryTreebank();
			for(Tree treeInstance : tBank){
				
					newTreebank.add(treeInstance);				
				
		
			}
			return newTreebank;
			
		}

	public static void main(String[] args) {
		
	
		// TODO Auto-generated method stub
		
		String expType = new String(args[0]);
		String dataPath = new String(args[1]);
		int sizeLimit = Integer.parseInt(args[2]);
		
		String brownPath = dataPath.concat("brown/");
		String wsjPath = dataPath.concat("wsj/");
		
		System.out.println(brownPath);
		System.out.println(wsjPath);
		//create parser
		myDaParser parserInstance = new myDaParser();
		
		FileFilter wsjFilter = new NumberRangesFileFilter("200-2277", true);
		parserInstance.wsj_02_22 = parserInstance.makeTreebank(wsjPath, wsjFilter);
		parserInstance.wsj_23 = parserInstance.makeTreebank(wsjPath.concat("23/"), null);
		
		parserInstance.splitBrownFromDir(brownPath);

		
		
		
		
		if(expType.equals("exp1")) {
			/*
			 * seed - WSJ (1000,2000....)
			 * seed test = WSJ sec 23
			 * selftrain 90% brown
			 * test  10% brown
			 */
			
			parserInstance.seedDataTreebank = parserInstance.reduceTreebank(parserInstance.wsj_02_22,sizeLimit);
			parserInstance.outDomainTestbank = parserInstance.convertToMembank(parserInstance.wsj_23);
			parserInstance.selfTrainTreebank = parserInstance.brown90Pc;
			parserInstance.testTreebank = parserInstance.brown10Pc;
			
		
		}
		else if(expType.equals("exp2")) {
			/*
			 * seed - WSJ first 10,000
			 * seed test = WSJ sec 23
			 * selftrain 1000,2000 ... in 90% brown
			 * test  10% brown
			 */
			parserInstance.seedDataTreebank = parserInstance.reduceTreebank(parserInstance.wsj_02_22,10000);
			parserInstance.outDomainTestbank = parserInstance.convertToMembank(parserInstance.wsj_23);
			parserInstance.selfTrainTreebank = parserInstance.reduceTreebank(parserInstance.brown90Pc,sizeLimit);
			parserInstance.testTreebank = parserInstance.brown10Pc;
			
		
		}
		else if(expType.equals("exp3")){
			/*
			 * seed - 90% brown 1000,2000,3000...
			 * seed test - 10% brown
			 * selftrain WSJ 02-22
			 * test  WSj 23
			 */
		
			parserInstance.seedDataTreebank = parserInstance.reduceTreebank(parserInstance.brown90Pc,sizeLimit);
			parserInstance.outDomainTestbank = parserInstance.brown10Pc;
			parserInstance.selfTrainTreebank = parserInstance.convertToMembank(parserInstance.wsj_02_22);
			parserInstance.testTreebank = parserInstance.convertToMembank(parserInstance.wsj_23);

		}
		else if(expType.equals("exp4")){
			
			/*
			 * seed - first 10,000  of 90% brown 
			 * seed test - 10% brown
			 * selftrain WSJ 02-22 1000,2000,3000....
			 * test  WSj 23
			 */
			parserInstance.seedDataTreebank = parserInstance.reduceTreebank(parserInstance.brown90Pc,10000);
			parserInstance.outDomainTestbank = parserInstance.brown10Pc;
			parserInstance.selfTrainTreebank = parserInstance.reduceTreebank(parserInstance.wsj_02_22,sizeLimit);
			parserInstance.testTreebank = parserInstance.convertToMembank(parserInstance.wsj_23);
		}
		
		

		 System.out.println(parserInstance.seedDataTreebank.size());
		 System.out.println(parserInstance.outDomainTestbank.size());
		 System.out.println(parserInstance.selfTrainTreebank.size());
		 System.out.println(parserInstance.testTreebank.size());

		 

		
		
		
		//create and train parser on seed data
		parserInstance.op = parserInstance.setParserOptions();
		parserInstance.trainParser(parserInstance.op);
		//create evaluator
		//parserInstance.createEvaluator();
		
		
		 
		System.out.println("VIDH: Testing on in domain data begin...... ");
		System.out.println("VIDH: size: "+parserInstance.outDomainTestbank.size());
		
		parserInstance.setTestingOptions("testOutput-prelim", "txt");
		//parserInstance.outDomainF1 = parserInstance.evaluate(parserInstance.outDomainTestbank);
	
		 EvaluateTreebank eval = new EvaluateTreebank(parserInstance.lp);
		 parserInstance.outDomainF1 =  eval.testOnTreebank(parserInstance.outDomainTestbank);
		 
		System.out.println("VIDH: Testing on in domain  data complete......");
		 	  
		 
		 
		 //parserInstance.inDomainF1 = parserInstance.evaluate(parserInstance.testTreebank);
		 
		 EvaluateTreebank eval_prelim = new EvaluateTreebank(parserInstance.lp);
		 parserInstance.inDomainF1 =  eval_prelim.testOnTreebank(parserInstance.testTreebank);
		 
		 System.out.println("VIDH: Self Training Data generation begin......");
		 parserInstance.setTestingOptions("selfTrainOutput", "txt");
		 parserInstance.getParseTreebankForSelfTraindata();
		 System.out.println("VIDH: size after getting parse trees: "+parserInstance.seedDataTreebank.size());
		 System.out.println("VIDH: Self Training Data generation complete......");

		  
		  
		  //DOMAIN ADAPTATION
		  System.out.println("VIDH: Merge Self Training Data with Seed data begin......");
		 // parserInstance.mergeSelfTrainingResults();
		  System.out.println("VIDH: size after merging: "+parserInstance.seedDataTreebank.size());
		  System.out.println("VIDH: Merge Self Training Data with Seed data complete......");
		  
		  System.out.println("VIDH: ReTraining begin...... ");
		  System.out.println("VIDH: size before retraining: "+parserInstance.seedDataTreebank.size());
		  
		 parserInstance.daop = parserInstance.setParserOptions();
		 LexicalizedParser retrainlp = LexicalizedParser.trainFromTreebank(parserInstance.seedDataTreebank,parserInstance.daop);

		  
		  System.out.println("VIDH: ReTraining complete......");			
			
		  System.out.println("VIDH: Testing on out of domain data begin......");
		  parserInstance.setTestingOptions("testOutput", "txt");
		  
		  
		  EvaluateTreebank eval_retrain = new EvaluateTreebank(retrainlp);
		  parserInstance.domainAdaptedF1 =  eval_retrain.testOnTreebank(parserInstance.testTreebank);

		  System.out.println("VIDH: Testing on out of domain data complete......");
		  		  

		  parserInstance.printResults();
		  
	}

}