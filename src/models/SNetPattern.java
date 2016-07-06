package models;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;

import communication.Constants;
import communication.ControllableThread;
import communication.MyLog;
import graphics.NetworkGraph;
import graphics.Surface;
import javafx.scene.shape.FillRule;
import neurons.BundleWeight;
import neurons.INeuron;
import neurons.ProbaWeight;
import sensors.Eye;

/**
 * 1st stage model: this network just predicts next input using probability weights.
 * Focus cannot change, no action occurs. Has short term memo
 * @author lana
 *
 */
public class SNetPattern implements ControllableThread {
	/** log */
	MyLog mlog = new MyLog("SNet", true);
	
	/** graphics*/
	Surface panel;
	/** net visualization */
	NetworkGraph netGraph;
	/** controls from UI */
	boolean paused = false;
	/** speed*/
	int speed = 1;
	
	/** data recording*/
	boolean save = true;
	/** the folder for this specific run*/
	String folderName;
	/** network parameter series */
	FileWriter paramWriter;
	/** neurons weights */
	FileWriter weightsWriter;
	/** bundle weights*/
	FileWriter bWeightsWriter;
	
	/**number of presentations for current image*/
	int presentations = 0;
	/**number of timesteps we train on each image*/
	int max_presentations = 1;
	/**Go to the next image*/
	boolean nextImage = false;
	/**current image*/
	int img_id = 0;
	/** time step (simulation time) */
	int step = 0;
	/** number of timesteps to stay on each image*/
	//int max_timesteps = 10;
	
	
	//environment
	/**images files*/
	String imagesPath = "/Users/lana/Desktop/prgm/JAVANeuron/JAVANeuron/src/images/";
	/** image description (chars)*/
	String[] images = {"ball_1","ball_2","ball_3","ball_1","ball_2_b","ball_4"}; //{"a","b","c"};		
	
	//sensors w/ actuators
	/** image sensor*/
	Eye eye;
	/** sensory neurons: [layer for this grayscale][id, neuron at different positions in the image] */
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];
	
	//neurons
	/**all neurons except eyes (sensory) so this is like "hidden layer"
	 * id, neuron*/
	HashMap<Integer, INeuron> allINeurons = new HashMap<Integer, INeuron>();
	/** total number of neuron ids*/
	int n_id = 0;
	/**short term memory, contains conscious neurons */
	Vector<INeuron> STM = new Vector<INeuron>();
	
	public SNetPattern(){
		//graphics
    	panel = new Surface();
    	panel.addControllable(this);
    	
		
    	//sensor init
    	eye = new Eye(imagesPath,panel);
		String iname = images[img_id];//+"_very_small";
    	eye.readImage(iname);
    	
    	//net creation
    	createNet();	
    	//net initialization
    	initNet();
    	//graphics
    	netGraph = new NetworkGraph((HashMap<Integer, INeuron>) allINeurons.clone());
	    netGraph.show();
    	
	    //init potential folder name
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    folderName = Constants.DataPath + "/" + strDate + "/";
    	if(save){
    		initDataFiles();
    	}
    	
		//main thread
    	new Thread(new ExperimentThread(this)).start();		
	}
	
	private void initDataFiles() {
		
	    //first create directory
		File theDir = new File(folderName);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    mlog.say("creating directory: " + folderName);
		    boolean result = false;
		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
		
		//now create csv files
		try {			
			//parameters
			paramWriter = new FileWriter(folderName+"/"+Constants.ParamFileName);
			mlog.say("stream opened "+Constants.ParamFileName);
        	String str = "iteration,neurons,connections\n";
        	paramWriter.append(str);
        	paramWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		//write initial weights
		writeWeights();
	}
	
	/** record weights in csv file*/
	private void writeWeights(){
		File theDir = new File(folderName);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    mlog.say("creating directory: " + folderName);
		    boolean result = false;
		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
		
		String weightsFileName = "weights_"+step+".csv";
		String bWeightsFileName = "bweights_"+step+".csv";
		
		try {
			weightsWriter = new FileWriter(folderName+"/"+weightsFileName);
			String str = "from,to,weight,age\n";
        	weightsWriter.append(str);
        	weightsWriter.flush();
        	
        	bWeightsWriter = new FileWriter(folderName+"/"+bWeightsFileName);
			str = "bid,from,to,weight,age\n";
        	bWeightsWriter.append(str);
        	bWeightsWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
			int id = 0;//bw
			while(it.hasNext()){
				Map.Entry<Integer, INeuron> pair = it.next();
				INeuron n = pair.getValue();
				//write outweights
				HashMap<INeuron,ProbaWeight> out = n.getOutWeights();
				Iterator<Entry<INeuron, ProbaWeight>> outit = out.entrySet().iterator();
				while(outit.hasNext()){
					Map.Entry<INeuron, ProbaWeight> outpair = outit.next();
					ProbaWeight w = outpair.getValue();
		        	String str = n.getId()+","+outpair.getKey().getId()+","+w.getProba()+ "," + w.getAge()+"\n";
		        	weightsWriter.append(str);					
		        	weightsWriter.flush();	
				}
				
				//bundles
				Vector<BundleWeight> bws = n.getDirectInWeights();
				for (Iterator<BundleWeight> iterator = bws.iterator(); iterator.hasNext();) {
					BundleWeight b = iterator.next();
					//"bid,from,to,weight,age\n";
					for (Iterator<INeuron> iterator2 = b.getInNeurons().iterator(); iterator2.hasNext();) {
						INeuron n2 = iterator2.next();
						//header = "bid,from,to,weight,age\n";
						String s = id + "," + n2.getId() + "," + n.getId() + "," + b.getProba() + "," + b.getAge() + "\n";
						bWeightsWriter.append(s);
						bWeightsWriter.flush();
					}
					id++;
				}
				
		    }
			weightsWriter.close();
			mlog.say("stream written "+weightsFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeParameters() {
		//count weights
		int nw = countWeights();
		//"iteration,neurons,connections\n";
		String str = step+","+allINeurons.size()+","+nw+"\n";
    	try {
			paramWriter.append(str);
	    	paramWriter.flush();	
		} catch (IOException e) {
			e.printStackTrace();
		}					
	}

	/**
	 * set the sizes of arrays and lists
	 */
	private void createNet(){
				
		//eye interfacing
		for(int i=0;i<eye_neurons.length;i++){
    		eye_neurons[i] = new HashMap<Integer, INeuron>();
    	}
		
		int n = eye.getPartialNeuronsNumber();
		mlog.say("eye has "+ n + " sensors");
	}
	
	
	/**
	 * fills list with neurons
	 */
	private void initNet(){
		int n_n = eye.getPartialNeuronsNumber();
		int gray_scales = Constants.gray_scales;
		
		//eye sensory neurons
		for(int i=0; i< n_n;i++){				
			for(int j=0;j<gray_scales;j++){
				//create neuron
				INeuron n = new INeuron(n_id);
				//put it in list
				eye_neurons[j].put(n_id, n);
				//make it sensitive to an input
				eye.linkNeuron(n_id,j, i);
				n_id++;				
				INeuron n2 = new INeuron(n_id);
				n2.justSnapped = true;//avoid snapping newborn neurons
				//add direct in weight
				Vector<INeuron> v = new Vector<INeuron>();
				v.addElement(n);
				BundleWeight b = n2.addDirectInWeight(v);
				//ProbaWeight p = n2.addInWeight(Constants.fixedConnection, n);
				n.addDirectOutWeight(n2,b);
				allINeurons.put(n_id, n2);
				n_id++;				
			}
		}	
		
		mlog.say(n_id +" neurons");		
	}
	
	
	/**
	 * resets all neurons activations,
	 * then builds the sensory inputs 
	 * and activate all outside weights and action_weights accordingly.
	 */
	public void buildInputs(){
		presentations++;
		if(presentations>=max_presentations){
    		nextImage = true;	
		}
		
		if(nextImage){
			presentations = 0;
			nextImage = false;
    		//change char
    		img_id++;
			if(img_id>=images.length){
				img_id=0;
			}
			String iname = images[img_id];//+"_very_small";
			eye.readImage(iname);
		}
		//build
		buildEyeInput();
	}
	
	
	int test = 0;
	/**
	 * Resets eye neurons activation to 0;
	 * builds the sensory input from the focused image,
	 * set graphics, 
	 * activates neurons in eye, activate corresponding weights
	 */
	private void buildEyeInput(){	 
		
		//reset activations of eye neurons and direct outweights
		for(int i=0;i<eye_neurons.length;i++){
			resetNeuronsActivation(eye_neurons[i]);
			resetDirectOutWeights(eye_neurons[i]);
		}
		//reset activations of ineurons
		resetNeuronsActivation(allINeurons);
		resetDirectOutWeights(allINeurons);

		
		//apply blur to selected portion of image
		//get grayscale values of the image
		int[] in = eye.buildCoarse(0,0);
		
		//go through sensory neurons and activate them.
		int n = in.length;
		int[][] n_interface = eye.getNeuralInterface();
		for(int k = 0; k<n; k++){
			//values in "in" start at 1, not 0
			int i = in[k]-1;//dont see white -1;
			//if(i>0){//dont see white
				eye_neurons[i].get(n_interface[i][k]).increaseActivation(1);
			//}
		}//*/

		/*if(test==0){
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			INeuron n2 = iterator.next().getValue();
			n2.increaseActivation(1);
			mlog.say("test is "+ test + " neuron " + n2.getId()+" is activated ");
			test++;
		}else if (test ==1 ){
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			iterator.next();
			INeuron n2 = iterator.next().getValue();
			n2.increaseActivation(1);
			mlog.say("test is "+ test + " neuron " + n2.getId()+" is activated ");
			//test = 0;
			test++;
		}else if (test == 2 ){
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			INeuron n2 = iterator.next().getValue();
			n2.increaseActivation(1);
			mlog.say("test is "+ test + " neuron " + n2.getId()+" and other are activated ");
			iterator.next();
			n2 = iterator.next().getValue();
			n2.increaseActivation(1);	
			mlog.say("test is "+ test + " neuron " + n2.getId()+" and other are activated ");
			test++;
		} else if (test == 3) {
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			iterator.next();
			iterator.next();
			iterator.next();
			iterator.next();
			iterator.next();
			INeuron n2 = iterator.next().getValue();
			n2.increaseActivation(1);		
			mlog.say("test is "+ test + " neuron " + n2.getId()+" is activated ");
			test = 0;
		}	
		//*/
		
		//propagate instantly from eye to 1st INeurons
		propagateFromEyeNeurons();

		//second pass for level 1 pattern neurons (bad)
		//TODO do this top-down after 1st sensory activation ? (maybe takes too much time??)
		Iterator<INeuron> it = allINeurons.values().iterator();
		while(it.hasNext()){
			INeuron nn =  it.next();
			nn.makeDirectActivation();
		}
		
		
		//integrate previously predicted activation to actual activation
		//integrateActivation();	
	}
	

	private void propagateFromEyeNeurons() {
		for (int i = 0; i < eye_neurons.length; i++) {
			Iterator<Entry<Integer, INeuron>> it = eye_neurons[i].entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, INeuron> pair = it.next();
				INeuron n = pair.getValue();
				if(n.getActivation()>0){
					n.activateDirectOutWeights();
				}
			}	
		}
		//activate corresponding neurons
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.makeDirectActivation();
		}
	}

	/**
	 * integrates previously predicted activation to actual activation
	 */
	private void integrateActivation() {
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.integrateActivation();
		}
	}


	
	/**
	 * for all neurons in this layer calculate probabilistic activation
	 * then activate all outside weights 
	 * if the neuron activation is above a threshold 
	 * @param layer
	 */
	private void activateOutWeights(HashMap<Integer, INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			if(ne.isActivated()){
				ne.activateOutWeights();
			}
		}
	}
	
	
	/**
	 * resets output weights activation to 0 in this layer
	 * @param layer
	 */
	public void resetOutWeights(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetOutWeights();
		}
	}
	
	/**
	 * reset direct output weights activation to 0 in this layer
	 * @param layer
	 */
	public void resetDirectOutWeights(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetDirectOutWeights();
		}
	}
	
	
	/**
	 * resets neurons activation to 0
	 * @param layer map of neurons to be reset.
	 */
	public void resetNeuronsActivation(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetActivation();
		}
	}
	
	

	/**
	 * update states of all neurons by propagating activation
	 * flow: age output weights, up input weights
	 */
	public void updateSNet() {			

		//update prediction probabilities		
				
		ageOutWeights(allINeurons);
		increaseInWeights(allINeurons);
		
		//add +1 value to the inweights if they were activated at t-1 & neuron is activated
		//increaseInWeights(allINeurons);
		//reset activation of all w
		for(int i=0;i<eye_neurons.length;i++){
			resetOutWeights(eye_neurons[i]);
		}		
		resetOutWeights(allINeurons);
		
		//for ineurons
		activateOutWeights(allINeurons);	
			
		calculateAndPropagateActivation();
		//create new weights based on (+) surprise
		makeWeights();
		
		//look at predictions
		buildPredictionMap();
				
		//update short term memory
		updateSTM();
		
		//input activations are reset and updated at the beginning of next step.
	}
	
	private void calculateAndPropagateActivation() {
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			n.calculateActivation();
			n.propagateActivation();
		}
		
	}

	/**
	 * empties old memories and put in conscious neurons.
	 * For now the memory span is 1 timestep only.
	 */
	private void updateSTM() {
		STM.clear();
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			//no hierarchy: all activated neurons are remembered, including sensory neurons.
			if(n.isActivated() & !n.isMute()){
				STM.add(n);
			}
		}
		
		//mlog.say("stm "+ STM.size());
	}

	/**
	 * create weights from previously conscious neurons to current surprised neurons.
	 * In this model there is no topological hierarchy yet, so all activated neurons are conscious.
	 */
	private void makeWeights() {
		if(STM.size()==0){
			mlog.say("just woke up");
			return;
		}
		
		//will store new neurons
		Vector<INeuron> newn = new Vector<INeuron>();
		
		//ineurons 
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		int nw = 0;
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.isSurprised()){
				
				//did we improve future prediction chances?
				boolean didChange = false;
				//go through STM
				for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
					INeuron preneuron = iterator.next();
					//if(n!=preneuron){//loops OK
						//doubloons weights will not be added
						ProbaWeight probaWeight = n.addInWeight(Constants.defaultConnection, preneuron);
						if(preneuron.addOutWeight(n,probaWeight)){
							nw++;
							didChange = true;
						}
					//}
				}
				
				//no change happened, try building a spatial pattern
				if(!didChange){					
					if(!patternExists(STM,n)){
						INeuron neuron = new INeuron(STM,n,n_id);
						newn.addElement(neuron);
						n_id++;
						mlog.say("created pattern neuron "+neuron.getId());
					}
				}
									
			}
		}
		
		for (Iterator<INeuron> iterator = newn.iterator(); iterator.hasNext();) {
			INeuron neuron = iterator.next();
			allINeurons.put(neuron.getId(), neuron);
		}
				
		mlog.say("added " + nw + " weights and "+ newn.size() + " neurons ");
		//writeWeights();
	}
	
	/**
	 * 
	 * @param neurons list of neurons
	 * @return true if there exists a PNeuron in the whole net that can be activated by the "neurons" pattern
	 * and has an outweight to to_n
	 */
	private boolean patternExists(Vector<INeuron> neurons, INeuron to_n) {
		boolean b = false;
		
		//unroll patterns
		HashSet<INeuron> allPatterns = new HashSet<INeuron>();
		for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			Vector<BundleWeight> bws = n.getDirectInWeights();
			if(bws.isEmpty()){
				allPatterns.add(n);
			}else {
				for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
					BundleWeight bw = iterator2.next();
					//there will not be duplicates
					allPatterns.addAll(bw.getInNeurons());
				}
			}
		}
		
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			if(n.sameBundleWeights(neurons,to_n)){
				b = true;
				break;
			}
			
			//check against unrolled patterns
			Vector<BundleWeight> bws = n.getDirectInWeights();
			for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
				BundleWeight bw = iterator2.next();
				if(bw.getInNeurons().containsAll(allPatterns)){
					b = true;
					break;
				}
			}
		}
		
		
		return b;
	}

	//TODO maybe we should use "certainty" as a predictor
	//and be surprised when we predicted stim but there was none??? (and somehow the neuron was not muted)
	//or just let it like this. We have 0.5 predictions that we don't know what will happen next.
	private void buildPredictionMap() {
		int n = Constants.gray_scales;
		
		//go through sensory neurons and build buffer
		int[][] n_interface = eye.getNeuralInterface();
		//black and white buffer for image
		//[row][column] = blackness level
		double[] coarse = new double[n_interface[0].length];
		//to calculate mean prediction
		int[] sum = new int[n_interface[0].length];

		//go through interface and build levels of gray
		for(int i=0; i<n_interface.length; i++){// i = gray scale
			for (int j = 0; j < n_interface[0].length; j++) {//j = position in image
				int n_id = n_interface[i][j];
				INeuron neuron = eye_neurons[i].get(n_id);
				//neuron.calculateActivation();
				if(neuron.getUpperPredictedActivation()>0){//
					//mlog.say(neuron.getId()+" inweight was activated ");
					sum[j]=sum[j]+1;
					//if white, dont't add anything
					// gray
					coarse[j] = coarse[j] + i;
				}
			}
		}		
		for (int i = 0; i < coarse.length; i++) {
			//normalize
			//i*i could have been added i times		
			coarse[i] = (coarse[i])/(n);//+1 bc grayscales start at 1 in eye
			//mlog.say(""+coarse[i]+" "+sum[i]);
			if(sum[i]>0){
				coarse[i] = coarse[i]/sum[i];
			}
		}

		//then put into image
		eye.setPredictedBuffer(coarse);		
	}
	
	/**
	 * age the outweights of neurons that are currently activated
	 */
	private void ageOutWeights(HashMap<Integer, INeuron> layer) {
		Iterator<INeuron> it = layer.values().iterator();
		while(it.hasNext()){
			INeuron n =  it.next();
			if(n.getActivation()>0){
				n.ageOutWeights();
			}
		}
	}
	
	/**
	 * increase the inweights of neurons that are currently activated
	 */
	private void increaseInWeights(HashMap<Integer, INeuron> layer) {
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.getActivation()>0){
				n.increaseInWeights();
			}
		}
	}

	
	/** 
	 * fuses similar neurons
	 * */
	private void snap() {
		mlog.say("snapping");
		int nw = countWeights();
		mlog.say("total connections "+ nw + " neurons "+ allINeurons.size());		
		
		ArrayList<INeuron> remove = new ArrayList<INeuron>();
		//go through net
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			
			if(!n.justSnapped){
				//look for equivalent neurons (neurons with equivalent outweights)
				Iterator<Entry<Integer, INeuron>> it2 = allINeurons.entrySet().iterator();
				while(it2.hasNext()){
					Map.Entry<Integer, INeuron> pair2 = it2.next();
					INeuron n2 = pair2.getValue();
										
					if((n.getId()!= n2.getId()) && !n2.justSnapped){
						boolean dosnap = true;

						//compare all out weights
						double diff = 0;
						int all = 0;
						HashMap<INeuron,ProbaWeight> out1 = n.getOutWeights();
						HashMap<INeuron,ProbaWeight> out2 = n2.getOutWeights();
						Iterator<Entry<INeuron, ProbaWeight>> out2it = out2.entrySet().iterator();
						//n1 must have all the weights that n2 has
						Set<INeuron> s1 = out1.keySet();
						Set<INeuron> s2 = out2.keySet();
						
						//avoid direct recurrent connections
						if(n.directInWeightsContains(n2) || n2.directInWeightsContains(n) ||
								//avoid different sets of outweights
								!s1.containsAll(s2) || !s2.containsAll(s1)){
							dosnap = false;
						} else {
							//compare outw
							while(out2it.hasNext()){
								Map.Entry<INeuron, ProbaWeight> out2pair = out2it.next();
								ProbaWeight w2 = out2pair.getValue();
								//can still learn: give up
								if(w2.canLearn()){
									dosnap = false;
									break;
								}
								
								//weight to same neuron; check value
								ProbaWeight w1 = out1.get(out2pair.getKey());
								if(w1.canLearn()){
									dosnap = false;
									break;//give up
								}
								diff += Math.pow(w1.getProba() - w2.getProba(),2);
								all++;
							}
							
							double dist = 0;//do snap even if there were no outweights at all
							if(all!=0){
								dist = Math.sqrt(diff)/all;					
							}
							//count how many connections are removed
							if(dist==0){//if exact same outweights
								//check if no direct contradiction in inweights (important)
								HashMap<INeuron,ProbaWeight> in1 = n.getInWeights();
								HashMap<INeuron,ProbaWeight> in2 = n2.getInWeights();
								Iterator<Entry<INeuron, ProbaWeight>> in1it = in1.entrySet().iterator();
								
								while(in1it.hasNext()){
									Map.Entry<INeuron, ProbaWeight> in1pair = in1it.next();
									ProbaWeight inw = in1pair.getValue();	
									if(in2.containsKey(in1pair.getKey())){
										//check that they do not individually contradict
										ProbaWeight inw2 = in2.get(in1pair.getKey());
										double indiff = Math.pow(inw.getProba() - inw2.getProba(),2);
										if(indiff>0.01){
											dosnap = false;//give up
										}
									}
								}
								Iterator<Entry<INeuron, ProbaWeight>> in2it = in2.entrySet().iterator();
								while(in2it.hasNext()){
									Map.Entry<INeuron, ProbaWeight> in2pair = in2it.next();
									ProbaWeight inw2 = in2pair.getValue();	
									if(in1.containsKey(in2pair.getKey())){
										//check that they do not individually contradict
										ProbaWeight inw1 = in1.get(in2pair.getKey());
										double indiff = Math.pow(inw1.getProba() - inw2.getProba(),2);
										if(indiff>0.01){
											dosnap = false;//give up
										}
									}
								}//*/
								
								if(dosnap){
									n.justSnapped = true;
									n2.justSnapped = true;
									remove.add(n2);
									
									//report n2 inputs to n if they did not exist
									n2.reportInWeights(n);
									
									//do the same for direct inweights
									n2.reportDirectInWeights(n);
				
									//now report direct outweights
									n2.reportDirectOutWeights(n);
									
									//notifies output neurons too
									n2.removeAllOutWeights();
									
									n2.clearDirectInWeights();
								}
							}

							
						}
					}
				}
			}
		}
		
		//count removed weights (only out weights)
		int g = 0;
		for(int i=0; i<remove.size();i++){	
			g = g+remove.get(i).getOutWeights().size();
			allINeurons.remove(remove.get(i).getId());
		}
		
		
		//reset "just snapped" values and remove ghost outweights
		it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.justSnapped = false;
			for(int i=0; i<remove.size();i++){
				if(n.removeOutWeight(remove.get(i))){
					g++;
				}
			}
		}
		
		nw = countWeights();
		mlog.say("after: weights "+ nw + " neurons " + allINeurons.size());
	}
	
	private int countWeights() {
		int nw = 0;
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			nw+= n.getOutWeights().size();
		}
		return nw;
	}
	
	//main thread
	//TODO should be in different class, maybe starter
	private class ExperimentThread implements Runnable {
		/** log */
		MyLog mlog = new MyLog("SNet Thread", true);
		/** network */
		SNetPattern net;
		
		public ExperimentThread(SNetPattern net){
			this.net = net;
		}
		
	    public void run() {
	    	try {
				Thread.sleep(000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  
	
	    	while(true){
	    		//pause
	    		if(!paused){
	    			
		    		long before = 0;
		    		if(step%20==0){
		    			//calculate runtime
		    			before = System.currentTimeMillis();
		    		}
		    		
		    		net.buildInputs();
		    		
					    
		    		net.updateSNet();
		    		int nw = countWeights();
		    		mlog.say("step " + step +" weights "+nw);
			
		    		if(step%20==0){
		    			long runtime = System.currentTimeMillis()-before;
		    			//save
		    			if(save){
		    				writeWeights();
		    				writeParameters();
		    			}
		    			//calculate snap time
		    			before = System.currentTimeMillis();
		    			net.snap();
		    			long snaptime = System.currentTimeMillis()-before;;
		    			mlog.say("runtime "+runtime + " snaptime "+ snaptime);
		    		}
		    		
		    		step++;
		    		
		    		//UI
				    panel.setTime(step);
				    //Vector<INeuron> v = new Vector<INeuron>(allINeurons.values());
				    netGraph.updateNeurons(allINeurons);				  
	    		}
	    		
	    		try {
					Thread.sleep(3000/speed);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    			    		
	    	}
	    }
	    		
	}

	@Override
	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	@Override
	public int speedUp() {
		speed++;	
		return speed;
	}

	@Override
	public int speedDown() {
		if(speed>1){
			speed--;
		}
		return speed;
	}

	@Override
	public void save(JButton saveButton) {
		//deactivate button
		saveButton.setEnabled(false);
		writeWeights();
		//reactivate button
		saveButton.setEnabled(true);	
	}

	@Override
	public void refresh() {
		netGraph.redraw((HashMap<Integer, INeuron>) allINeurons.clone());
	}

}

