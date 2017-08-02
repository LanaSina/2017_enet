package models;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import javax.swing.JButton;

import communication.Constants;
import communication.ControllableThread;
import communication.MyLog;
import communication.Utils;
import graphics.NetworkGraph;
import graphics.Surface;
import neurons.BundleWeight;
import neurons.INeuron;
import neurons.ProbaWeight;
import sensors.Eye;

/**
 * 1st stage model: this network just predicts next input using probability weights.
 * Focus cannot change, no action occurs. Has short term memory
 * @author lana
 *
 */
public class SNetPattern implements ControllableThread {
	/** log */
	MyLog mlog = new MyLog("SNet", true);
	ProbaWeight testp;
	
	/** data recording*/
	boolean save = true;
	
	/** graphics*/
	Surface panel;
	/** net visualization */
	NetworkGraph netGraph;
	/** controls from UI */
	boolean paused = false;
	/** speed*/
	int speed = 1;
	boolean draw_net = true;
	/** max number of new connections per step*/
	int max_new_connections = 5000;
	/** max inweights per neuron */
	//int max_in_weights = 500;
	int max_total_connections = 50000;
	//int max_layers = 10;//6
	boolean cpu_limitations = false;
	boolean add_weights = true;


	/** the folder for this specific run*/
	String folderName;
	/** network parameter series */
	FileWriter net_param_writer;
	/** neurons weights */
	FileWriter weightsWriter;
	/** bundle weights*/
	FileWriter bWeightsWriter;
	/** performance (surprise)*/
	FileWriter perfWriter;
	/** memories */
	FileWriter memWriter;
	
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
	
	/** phase */
	boolean dreaming = false;
	/** number of activated sensory neurons*/
	int activated = 0;

	//environment
	/**images files*/
	String imagesPath = "/Users/lana/Desktop/prgm/SNet/images/ball/variation/"; 
	/** leading zeros*/
	String name_format = "%02d";
	/** number of images*/
	int n_images = 6;//
	
	//sensors 
	/** image sensor*/
	Eye eye;
	/** sensory neurons: [layer for this grayscale][id, neuron at different positions in the image] */
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];

	//neurons
	/**all neurons except eyes (sensory) so this is like "hidden layer"
	 * id, neuron*/
	HashMap<Integer, INeuron> allINeurons = new HashMap<Integer, INeuron>();
	/** id range of initial (sensory) INeurons (used to calculate surprise)*/
	int si_start = 0;
	int si_end = 0;
	/** total number of neuron ids*/
	int n_id = 0;
	/**short term memory, contains conscious neurons */
	Vector<INeuron> STM = new Vector<INeuron>();
	
	//memory
	BufferedReader memReader;
	boolean readingMemory = false;
	File memoryFile;
	
//actions
//	/**vector of ids of activated muscles (id must correspond to motion value in Eye)*/
//	Vector<Integer> h_muscles = new Vector<Integer>();
//	/** vector of ids of activated muscles (id must correspond to motion value in Eye)*/
//	Vector<Integer> v_muscles = new Vector<Integer>();
//	//proprioception
//	/** proprioceptive neurons (horizontal eye muscle)*/
//	ArrayList<INeuron> eyepro_h = new ArrayList<INeuron>();
//	/** proprioceptive neurons (vertical eye muscle)*/
//	ArrayList<INeuron> eyepro_v = new ArrayList<INeuron>();
//	/** id range of propriocetptive INeurons \*/
//	int pi_start = 0;
//	int pi_end = 0;

	
	public SNetPattern(){
		//init potential folder name
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    folderName = Constants.DataPath + "/" + strDate + "/";
    	
    	//graphics
    	panel = new Surface();
    	panel.addControllable(this);  
    	
    	//sensor init
    	eye = new Eye(imagesPath, panel);
    	//leading zeros
		String iname =  String.format(name_format, img_id); //images[img_id];//"%010d"
    	eye.readImage(iname);
    	
    	//net creation
    	createNet();	
    	//net initialization
    	initNet();
    	
    	//graphics
    	if(draw_net){
	    	netGraph = new NetworkGraph((HashMap<Integer, INeuron>) allINeurons.clone(), eye_neurons, eye);
		    netGraph.show(); 
	    	panel.setGraph(netGraph);
    	}
    	
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
			//run parameters
			FileWriter param_writer = new FileWriter(folderName+"/"+Constants.Param_file_name);
			mlog.say("stream opened "+Constants.Param_file_name);
        	String str = "max_presentations,image_files,sensory_neurons,hidden_neurons,stm,"
        			+ "eye_noise,noise_range,noise_rate, max_layers,"
        			+ "focus_resolution, nonfocus_resolution, grayscales,max_new_connections,max_in_weights\n";
        	param_writer.append(str);
        	str = ""+max_presentations + "," +  n_images + "," +  eye_neurons.length*eye_neurons[0].size() +
        			"," + allINeurons.size() + "," + STM.size() + 
        			"," + eye.has_noise + "," + eye.noise_rate + "," + eye.noise_rate + "," + "infinite" + ","+
        			+ Constants.eres_f + "," + Constants.eres_nf + "," + Constants.gray_scales +","+ /*max_new_connections*/ "infinite" + "," + /*max_in_weights*/ "infinite" + "\n";
        	param_writer.append(str);
        	param_writer.flush();
        	param_writer.close();
        	
			//parameters
			net_param_writer = new FileWriter(folderName+"/"+Constants.Net_param_file_name);
			mlog.say("stream opened "+Constants.Net_param_file_name);
        	str = "iteration,neurons,connections\n";
        	net_param_writer.append(str);
        	net_param_writer.flush();
        	
        	//performance and surprise 
        	perfWriter = new FileWriter(folderName+"/"+Constants.PerfFileName);
			mlog.say("stream opened "+Constants.PerfFileName);
        	str = "iteration,error,surprise,illusion \n";
        	perfWriter.append(str);
        	perfWriter.flush();
        	
        	//memory
        	memWriter = new FileWriter(folderName+"/"+Constants.MemoryFileName);
			mlog.say("stream opened "+Constants.MemoryFileName);
        	str = "iteration,id\n";
        	memWriter.append(str);
        	memWriter.flush();
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
		int nw = Utils.countWeights(allINeurons);
		//"iteration,neurons,connections\n";
		String str = step+","+allINeurons.size()+","+nw+"\n";
    	try {
			net_param_writer.append(str);
	    	net_param_writer.flush();	
		} catch (IOException e) {
			e.printStackTrace();
		}					
	}
	

	/**
	 * @param p performance
	 * @param s surprise
	 * @param i illusions
	 */
	private void writeError(double p, double s, double i) {
		String str = step + "," + p + "," + s + "," + i +"\n";
    	try {
			perfWriter.append(str);
	    	perfWriter.flush();	
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
		si_start = n_id;
		int[][] eye_interface = eye.getEyeInterface();
		
		//eye sensory neurons
		for(int i=0; i< n_n;i++){				
			for(int j=0;j<gray_scales;j++){
				//create neuron
				INeuron n = new INeuron(n_id);
				//put it in list
				eye_neurons[j].put(n_id, n);
				//position
				int sensor_i = eye_interface[i][0]+(n_n*j);
				int sensor_j = eye_interface[i][1]+(n_n*j);
				double[] p = {sensor_i+1,sensor_j+1,0,0};
				n.setPosition(p);
				//make it sensitive to an input
				eye.linkNeuron(n_id,j, i);
				n_id++;				
				
				INeuron n2 = new INeuron(n_id);
				//add direct in weight
				Vector<INeuron> v = new Vector<INeuron>();
				v.addElement(n);
				BundleWeight b = n2.addDirectInWeight(v);
				n.addDirectOutWeight(n2,b);
				allINeurons.put(n_id, n2);
				//mlog.say("created+ " + n_id);
				n_id++;				
			}
		}	
		si_end = n_id-1;

		mlog.say("Initial INeurons: id "+ si_start + " to "+ si_end);
		if(allINeurons.containsKey(0)){
			mlog.say("contains 0");
		}
		
		/*pi_start = n_id;
		//move right or left 
		for(int i=0; i< eye.getHorizontalMotionResolution();i++){	
			INeuron n = new INeuron(n_id);
			double[] p = {-i-1,0,0,0};
			n.setPosition(p);
			eyepro_h.add(n);	
			allINeurons.put(n.getId(), n);
			//mlog.say("Proprioception INeuron " + n_id);
			n_id++;			
		}	
		
		//up or down
		for(int i=0; i< eye.getVerticalMotionResolution();i++){	
			INeuron n = new INeuron(n_id);
			double[] p = {-i-1,1,0,0};
			n.setPosition(p);
			eyepro_v.add(n);	
			allINeurons.put(n.getId(), n);
			//mlog.say("Proprioception INeuron " + n_id);
			n_id++;
		}
		pi_end = n_id-1;
		
		mlog.say("Proprioception INeurons: id "+ pi_start + " to "+ pi_end);*/
		
		mlog.say(n_id +" neurons");		
		
		resetNeuronsActivation(allINeurons);
	}
	
	
	/**
	 * resets all neurons activations,
	 * then builds the sensory inputs 
	 * and activate all outside weights and action_weights accordingly.
	 */
	public void buildInputs(){
		if(!readingMemory){
			if(nextImage){
				presentations = 0;
				nextImage = false;
	    		//change char
	    		img_id++;
				if(img_id>=n_images){
					img_id=0;
				}
				String iname =  String.format(name_format, img_id); 
				eye.readImage(iname);
			}
			//build
			buildEyeInput();
	
			presentations++;
			if(presentations>=max_presentations){
	    		nextImage = true;	
			}//*/
		}else{
			//reset
			resetNeuronsActivation(allINeurons);
			Utils.resetDirectOutWeights(allINeurons);
			
			//read and activate all neurons in memory
			try {
				String line = memReader.readLine();
				if(line == null){
					//start from 0
					memReader.close();
					loadMemory();
					line =  memReader.readLine();
				}
				
				int st = -1;
				String[] info;
				info = line.split(",");
				mlog.say("line* " + line);
				st = Integer.valueOf(info[0]);
				int i = Integer.valueOf(info[1]);
				while(i==-1 && !(line==null)){
					line =  memReader.readLine();
					mlog.say("line* " + line);
					info = line.split(",");
					i = Integer.valueOf(info[1]);
					st = Integer.valueOf(info[0]);
				}
				
				if(line == null){
					//start from 0
					memReader.close();
					loadMemory();
					line =  memReader.readLine();
				}
				
				step = st;
				int st2 = st;
				INeuron n;
				while (true) {
					//"iteration, ID\n";
					info = line.split(",");
					st2 = Integer.valueOf(info[0]);
					if(st2!=st){
						//new line of iteration is just filling: ok to skip it
						break;
					}else{
						//ignore absent neurons
						if((n = allINeurons.get(Integer.valueOf(info[1])))!=null){
							n.increaseActivation(1);
						}
					}
					line = memReader.readLine();
					if(line==null){
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			integrateActivation();	
			Utils.propagateInstantaneousActivation(allINeurons.values());
		}
		
		//choose actions, activate "proprioceptive" neurons, act at next step
		//findActions();
	}
	
	
	int test = 0;
	/**
	 * Resets eye neurons activation to 0;
	 * builds the sensory input from the focused image,
	 * set graphics, 
	 * activates neurons in eye, activate corresponding weights
	 */
	private void buildEyeInput(){	 
		//for performance calculation
		//number of surprised neurons at this timestep
		int n_surprised = 0;
		//number of sensory activates
		int n_activated = 0;
		//predicted, not activated
		int n_illusion = 0;
		
		//reset activations of eye neurons and direct outweights
		for(int i=0;i<eye_neurons.length;i++){
			resetNeuronsActivation(eye_neurons[i]);
			Utils.resetDirectOutWeights(eye_neurons[i]);
		}
		
		//reset activations of ineurons
		resetNeuronsActivation(allINeurons);
		Utils.resetDirectOutWeights(allINeurons);
		
		if(!dreaming){
			//apply blur to selected portion of image
			//get grayscale values of the image
			int[] in = eye.buildCoarse();
			
			
			//go through sensory neurons and activate them.
			int n = in.length;
			int[][] n_interface = eye.getNeuralInterface();
			for(int k = 0; k<n; k++){
				//values in "in" start at 1, not 0
				int i = in[k]-1;
				INeuron eyen = eye_neurons[i].get(n_interface[i][k]);
				if(i>=0){//>=0 if seeing white
					eyen.increaseActivation(1);
					n_activated++;
					if(eyen.getUpperSurprised()){
						n_surprised++;
					}
				}
				
				if(eyen.getUpperIllusion()){
					n_illusion++;
				}
			}//*/
		}else{
			
			if(step%10==0){
				//deactivate all neurons
				deactivateAll();
				//activated = 0;
			}
			
			activated = getActivated();

			Object[] neurons = allINeurons.values().toArray();
			int max = neurons.length;
			int total = (max/20) - activated;//don't overload net
			if(total<0){
				deactivateAll();
				activated = 0;
			}
			
			mlog.say("total "+ total + " activated "+ activated);
			for(int i=0; i<total; i++){
				int l =  (int) Constants.uniformDouble(0,max);
				((INeuron) neurons[l]).increaseActivation(1);
				((INeuron) neurons[l]).calculateActivation();
			}
			
			integrateActivation();	
			Utils.propagateInstantaneousActivation(allINeurons.values());
		}
		
		//proprioception here works before actual contraction
		//useful bc behaviour is random, but in the future proprioception can happen
		//during motion as normal
		//send order to eye
		/*int[] proprio = eye.contractMuscles(v_muscles, h_muscles);
		int v_m = proprio[0];
		int h_m = proprio[1];

		INeuron np = eyepro_v.get(v_m+1);
		np.increaseActivation(1);
		np = eyepro_h.get(h_m+1);
		np.increaseActivation(1);
		mlog.say("intention "+np.getId());

		String action = "h "+ h_m + " v " + v_m;
		panel.setAction(action);*/

		//propagate instantly from eyes
		for (int i = 0; i < eye_neurons.length; i++) {
			Utils.propagateInstantaneousActivation(eye_neurons[i].values());
		}
		/*Utils.propagateInstantaneousActivation(eyepro_h);
		Utils.propagateInstantaneousActivation(eyepro_v);*/

		if(save){	
			double error, surprise, illusion;
			if(n_activated == 0){
				surprise = 0;
				illusion = 0;
				error = -1;
			}else{
				surprise = (n_surprised*1.0/n_activated);//false negatives
				illusion = (n_illusion*1.0/n_activated); //false positives
				error = surprise + illusion;
			}
			writeError(error, surprise, illusion);
		}
		mlog.say(" surprised: " + n_surprised + " illusions " + n_illusion + " activated " + n_activated);
	}
	
	private void deactivateAll() {
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			n.resetActivation();
			n.resetDirectOutWeights();
			n.resetOutWeights();
			//shouldnt be needed but is?
			n.resetInWeights();
			n.setMute(false);
		}
	}
	
	private int getActivated(){
		int t = 0;
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			n.calculateActivation();
			if(n.isActivated()){
				t++;
			}
		}
		return t;
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
	 * resets neurons activation to 0
	 * TODO delete this and use function below
	 * @param layer map of neurons to be reset.
	 */
	public void resetNeuronsActivation(HashMap<Integer,INeuron> layer){
		Utils.resetNeuronsActivation(layer.values());
	}

	
	

	/**
	 * update states of all neurons by propagating activation
	 * flow: age output weights, up input weights
	 */
	public void updateSNet() {			

		if(!readingMemory){
			//update prediction probabilities		
			Utils.ageOutWeights(allINeurons);
			Utils.increaseInWeights(allINeurons);
			//reset activation of all w
			for(int i=0;i<eye_neurons.length;i++){
				Utils.resetOutWeights(eye_neurons[i]);
			}	
		}
			
		Utils.resetOutWeights(allINeurons);
		
		//for ineurons
		Utils.activateOutWeights(allINeurons);	
		//muting happens here
		//predicted activation for next step calculated here
		Utils.calculateAndPropagateActivation(allINeurons);
		
		//create new weights based on surprise
		if(!readingMemory){
			makeWeights();
		}
		
		//look at predictions
		buildPredictionMap();
				
		//update short term memory
		updateSTM();
		
		//input activations are reset and updated at the beginning of next step.
	}
	

	/**
	 * empties old memories and put in conscious neurons.
	 * For now the memory span is 1 timestep only.
	 */
	private void updateSTM() {
		STM.clear();
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		
		String str = step + ",-1\n";//filler line
    	try {
    		memWriter.append(str);
    		memWriter.flush();	
    	} catch (IOException e) {
			e.printStackTrace();
		}	
    	
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			//no hierarchy: all activated neurons are remembered, including sensory neurons.
			double[] p = n.getPosition();
			if(p[0] == 0 && p[1] == 0){
				mlog.say("============ bad position " + n.getId());
			}
			
			if(n.isActivated() & !n.isMute()){
				STM.add(n);
				//memories
				if(n.isSurprised()){
			    	try {
			    		str = step+","+ n.getId()+"\n";
			    		memWriter.append(str);
			    		memWriter.flush();	
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
			    
		    	//co-activation
				Utils.updateCoactivation(n,allINeurons);
			}
			
			
		}
	}

	/**
	 * create weights from previously conscious neurons to current surprised neurons.
	 * Beware: muted neurons at t can actually be in the STM (t-1)
	 */
	private void makeWeights() {
		
		//will store new neurons
		Vector<INeuron> newn = new Vector<INeuron>();
		
		//in case we made a pattern neuron
		INeuron the_pattern = null;
	
		if(STM.size()==0){
			mlog.say("just woke up");
			if(save){
				writeError(0,0,0);
			}
			return;
		}	
		//make intra stm weights
		for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			for (Iterator<INeuron> iterator2 = STM.iterator(); iterator2.hasNext();) {
				INeuron n2 = iterator2.next();
				
				//no self co-activation
				if(n2!=n){
					if(!n.getCoWeights().containsKey(n2)){
						//add it
						ProbaWeight w = new ProbaWeight(Constants.defaultConnection);
						w.setValue(1);
						n.getCoWeights().put(n2, w);
						n2.getInCoWeights().put(n, w);
					}
				}
			}
		}
		
		
		//ineurons 
		//todo make order random
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		int nw = 0;
		int total = Utils.countWeights(allINeurons);		
		
		if(add_weights){
			while(it.hasNext()){
				Map.Entry<Integer, INeuron> pair = it.next();
				INeuron n = pair.getValue();
				/*int id = n.getId();
				//do not try to predict proprioception: action choice is random for now
				if(id>=pi_start && id<=pi_end){
					continue;
				}*/
				
				if(n.isSurprised()){// && !n.isMute() must predict activation of small ones too
					
					//did we improve future prediction chances?
					boolean didChange = false;
					
					//go through STM
					for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
						INeuron preneuron = iterator.next();
						
						
						if((cpu_limitations && nw>max_new_connections)) break;
						
						//doubloons weights will not be added
						if(!allINeurons.containsKey(preneuron.getId())){
							//throw new Error("not in collection "+preneuron.getId()); ok for now
						}else{
							ProbaWeight probaWeight = n.addInWeight(Constants.defaultConnection, preneuron);
							if(preneuron.addOutWeight(n,probaWeight)){
								probaWeight.setActivation(1,null);
								nw++;
								didChange = true;
							}
						}
						
						//check for oversnapping in sensory neurons
						//todo: generalize to normal neurons and pattern neurons
						/*if(preneuron.getDirectInWeights().size()>1){
							didChange = true;
							Vector<BundleWeight> v = preneuron.getDirectInWeights();
							for (Iterator<BundleWeight> v_it = v.iterator(); v_it.hasNext();) {
								if(nw>max_new_connections) break;
								BundleWeight bundleWeight = v_it.next();
								//create separate neuron for each non activated weight
								//we don't know which weights were activated
								Vector<INeuron> vect = new Vector<>(bundleWeight.getInNeurons());
								INeuron unsnapped = new INeuron(vect,n,n_id);
								n_id++;
								nw++;
								newn.addElement(unsnapped);
								//what to do with existing probability???
								//TODO not just "forget" it, bad
								mlog.say("---- unsnapped ");
							}
							//remove original neuron
							preneuron.removeAllOutWeights();						
							preneuron.clearDirectInWeights();
							remove.add(preneuron);
							preneuron.setMute(true);//to ignore it in next loops
						}*/
					}	
					
					//no change, try pruning spatial patterns
					if(!didChange){
						//look at input neuron's bundles vs STM
						for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
							INeuron preneuron = iterator.next();
							HashMap<INeuron, ProbaWeight> inw = n.getInWeights();
							if(inw.containsKey(preneuron) && inw.get(preneuron).getProba()>Constants.confidence_threshold){
								//look at those that were activated (ie in STM)
								Vector<BundleWeight> pr = preneuron.getDirectInWeights();
								for (Iterator<BundleWeight> iterator2 = pr.iterator(); iterator2.hasNext();) {
									BundleWeight bundleWeight = iterator2.next();
									
									Set<INeuron> bn = bundleWeight.getBundle().keySet();
									Vector<INeuron> newBundle = new Vector<>();
									for (Iterator<INeuron> iterator3 = bn.iterator(); iterator3.hasNext();) {
										INeuron iNeuron =  iterator3.next();
										if(STM.contains(iNeuron)){
											newBundle.addElement(iNeuron);
										}
									}
									if (newBundle.size()>=2) {
										bundleWeight.decreaseAllBut(newBundle);
										didChange = true;
										//mlog.say("Degreasing bundle from "+preneuron.getId() + " to " + n.getId());
									}
								}
							}
						}
					}//*/
					
					
					//no change happened, try building a spatial pattern
					if(!didChange){// && !dreaming){	//  
						if(cpu_limitations && nw>max_new_connections) break;
						if(true){//!hasMaxLayer(STM)
							Vector<INeuron> vn = Utils.patternExists3D(STM, n);
							if(vn.size()>0){
								if(the_pattern==null){
									if(vn.size()>1){
										
										//INeuron 
										the_pattern = new INeuron(vn,n,n_id);
										n_id++;
										newn.addElement(the_pattern);
										ProbaWeight weight = the_pattern.getOutWeights().get(n);
										weight.setActivation(1, null);
										mlog.say("******** added pattern neuron");
									}/*else{
										//only used when debugging
										INeuron pn = vn.get(0);
										ProbaWeight p = n.addInWeight(Constants.defaultConnection, pn);
										pn.addOutWeight(n, p);
										p.setActivation(1, null);
										mlog.say("******** added p weight from "+ pn.getId() + " to " + n.getId());
									}*/
									
									nw++;
									didChange = true;
								} else{
									ProbaWeight p = n.addInWeight(Constants.defaultConnection, the_pattern);
									if(the_pattern.addOutWeight(n, p)){
										nw++;
										didChange = true;
										p.setActivation(1, null);
										mlog.say("******** added pattern weight to " + n.getId());
									}
								}//*/
							}
						}
					}
					
					//if it changed, it is good to recalculate predicted activation
					if(didChange){
						n.activationCalculated = false;
						n.calculateActivation();
						n.setSurprised(true);
					}
				}
			}
		}else{
			mlog.say("network too big");
		}
		
		if(testp != null){
			mlog.say("+++++++ proba "+testp.getProba());
		}
		
		if(cpu_limitations){
			if(total+nw>max_total_connections){
				add_weights = false;
			}else{
				add_weights = true;
			}
		}
		
		
		for (Iterator<INeuron> iterator = newn.iterator(); iterator.hasNext();) {
			INeuron neuron = iterator.next();
			allINeurons.put(neuron.getId(), neuron);
		}
		
		if(draw_net && nw>0){
			netGraph.setHiddenLayer(allINeurons);
		}
				
		mlog.say("added " + nw + " weights and "+ newn.size() + " neurons ");
		
	}
	
	/**
	 * checks if STM has a neuron from the maximum authorized layer
	 * @param sTM2
	 * @return
	 */
	private boolean hasMaxLayer(Vector<INeuron> sTM2) {
		boolean has = false;
		for (Iterator<INeuron> iterator = sTM2.iterator(); iterator.hasNext();) {
			INeuron iNeuron = iterator.next();
			/*if(iNeuron.level>=max_layers){
				has = true;
				//mlog.say("layer limit");
			}*/
		}
		return has;
	}

	
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
		for(int i=0; i<Constants.gray_scales; i++){// i = gray scale //n_interface.length
			for (int j = 0; j < n_interface[0].length; j++) {//j = position in image
				int n_id = n_interface[i][j];
				INeuron neuron = eye_neurons[i].get(n_id);
				//if the neuron is not muted, get its prediction
				//never muted: these are eye neurons
				if(neuron.getUpperPredictedActivation()>0){//
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
			if(sum[i]>0){
				coarse[i] = coarse[i]/sum[i];
			}
		}
		
		//then put into image
		eye.setPredictedBuffer(coarse);		
	}

	
	/** 
	 * fuses similar neurons
	 * */
	private void snap() {
		//cleanAll();
		allINeurons = Utils.snap(allINeurons);
	}
	
	private void cleanAll() {
		STM.clear();
		deactivateAll();
	}
	
	
	/**
	 * decide of the next action to do
	 */
	/*private void findActions() {
		//array of "intentions"
		//in the end, actions will be "flavored" to allow choice
		
		//random eye actions
		h_muscles.clear();
		int act =  (int) Constants.uniformDouble(0,3);//0..2
		h_muscles.addElement(act);
		
		v_muscles.clear();
		act =  (int) Constants.uniformDouble(0,3);
		v_muscles.addElement(act);
	}*/
	
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
		    		if(step%Constants.snap_freq==0){
		    			//calculate runtime
		    			before = System.currentTimeMillis();
		    		}
		    		
		    		//also unmutes neurons
		    		net.buildInputs();
  
		    		net.updateSNet();
		    		int nw = Utils.countWeights(allINeurons);
		    		mlog.say("step " + step +" weights "+nw);
			
		    		if(!readingMemory){
			    		if(step%Constants.snap_freq==0){
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
			    			mlog.say("runtime "+runtime + " snaptime "+ snaptime);//*/
			    			
			    			//sleep
			    			/*if(step>1){
				    			if(!dreaming){
				    				dreaming = true;
				    				cleanAll();
				    				mlog.say("********* dreaming");
				    			}else{	    				
				    				dreaming = false;
				    				cleanAll();
				    				mlog.say("********* not dreaming");
				    			}
			    			}//*/
			    		}
		    		
		    			step++;
		    		}
		    		
		    		//UI
				    panel.setTime(step);
				    if(draw_net){
				    	netGraph.setHiddenLayer(allINeurons);
				    	//netGraph.updateNeurons();
				    }
				    
				    try {
		    			if(draw_net){
		    				Thread.sleep(3000/speed);
		    			}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}    	
	    		} else{
	    			try {
		    			Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}    	
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
		
		//create folder
		
		//write weights
		//writeWeights();
		
		//save net
		Vector<INeuron> sensors = new Vector<INeuron>();
		for(int i=0; i<eye_neurons.length; i++){
			sensors.addAll(eye_neurons[i].values());
		}
		Utils.saveNet(allINeurons, sensors, folderName, step);
		
		//reactivate button
		saveButton.setEnabled(true);	
	}

	@Override
	public void refresh() {
		if(draw_net){
			netGraph.redraw();
		}
	}
	
	private void loadNetwork(File file) {

		//get correct names
		File sensor_file, net_file, pos_file;
		mlog.say("file parent name "+ file.getParent());
		if(file.getName().equals(Constants.Sensors_file_name)){
			sensor_file = file;
			String net_name = file.getParent() + "/" + Constants.Net_file_name;
			net_file = new File(net_name);
			pos_file = new File(file.getParent() + "/" + Constants.Positions_file_name);
		}else if(file.getName().equals(Constants.Net_file_name)){
			net_file = file;
			String sensor_name = file.getParent() + "/" + Constants.Sensors_file_name;
			sensor_file = new File(sensor_name);
			pos_file = new File(file.getParent() + "/" + Constants.Positions_file_name);
		} else{
			pos_file = file;
			String net_name = file.getParent() + "/" + Constants.Net_file_name;
			net_file = new File(net_name);
			String sensor_name = file.getParent() + "/" + Constants.Sensors_file_name;
			sensor_file = new File(sensor_name);
		}
		
		mlog.say("sensors "+ sensor_file.getName());
		mlog.say("net "+ net_file.getName());
		mlog.say("position  "+ pos_file.getName());

		//clean up everything	
		allINeurons.clear();
		STM.clear();		
		HashMap<Integer, INeuron> sensors = new HashMap<Integer, INeuron>();
		for(int i=0; i<eye_neurons.length; i++){
			Iterator<INeuron> it = eye_neurons[i].values().iterator();
			while (it.hasNext()) {
				INeuron n = it.next();
				sensors.put(n.getId(), n);
				n.clearDirectOutWeights();
			}
		}
		
		try {
			//build all neurons and set all positions
	        //network
			BufferedReader br = new BufferedReader(new FileReader(pos_file));
	        String line = br.readLine();//skip 1 line
	        mlog.say(line);
	        int maxid = -1;
	        while ((line = br.readLine()) != null) {
	            // use comma as separator
				//str = "ID, x, y, sx, sy\n";
	            String[] info = line.split(",");
	            int nid = Integer.valueOf(info[0]);
	            if(nid>maxid) maxid = nid;
            	
	            INeuron n = new INeuron(nid);
            	double[] p = {Double.valueOf(info[1]),Double.valueOf(info[2]),
            			Double.valueOf(info[3]),Double.valueOf(info[4])};
				n.setPosition(p);
				allINeurons.put(n.getId(), n);
	        }
	        br.close();
	        n_id = maxid+1;
	        	        
			//sensors-to-network links
	        //maybe we dont need this?
			br = new BufferedReader(new FileReader(sensor_file));
			line = br.readLine();//skip 1 line
			int neuron_id = -1;
            INeuron n = null;
	        while ((line = br.readLine()) != null) {
	            // use comma as separator
	            String[] info = line.split(",");
	            //"sensor_ID, neuron_ID, x, y, sx, sy \n";
	            if(Integer.valueOf(info[1])!=neuron_id){
	            	neuron_id = Integer.valueOf(info[1]);
	            	n = allINeurons.get(neuron_id);
	            	if(n==null){
						mlog.say("not found " + neuron_id);
	            	}
	            }
	            	
	        	//add direct in weight
	        	INeuron s = sensors.get(Integer.valueOf(info[0]));
				Vector<INeuron> v = new Vector<INeuron>();
				v.addElement(s);
				BundleWeight b = n.addDirectInWeight(v);
				s.addDirectOutWeight(n, b);
	        }
	        br.close();

	        //network
	        br = new BufferedReader(new FileReader(net_file));
	        int id = -1;
	        int w_id = -1;
	        BundleWeight bw = null;
	        n = null;
	        line = br.readLine();//skip 1 line
	        while ((line = br.readLine()) != null) {
	            // use comma as separator
	            String[] info = line.split(",");
	            //"ID, weight_type, weight_id, in_neuron, value, age\n";
	            int id2 = Integer.valueOf(info[0]);
	            if(id!=id2){
	            	id = id2;
	    	        w_id = -1;
	            	n = allINeurons.get(id);
	            	//mlog.say("do "+id);
	            	if(n==null){
						mlog.say("not found " + id);
	            	}
	            }
	            
	            String w_type = info[1];
	            if(w_type.equals("direct")){
	            	int w_id2 = Integer.valueOf(info[2]);
	            	if(w_id2!=w_id){
	            		w_id = w_id2;
	            		//new bundle
	            		bw = new BundleWeight(Constants.fixedConnection);
	            		n.addDirectInWeight(bw);
	            		//mlog.say("added bundle weight to " + n.getId());
	            	}
	            	
	            	int in_id = Integer.valueOf(info[3]);
	            	//ignore sensors
	            	if(sensors.containsKey(in_id)){
	            		continue;
	            	}
	            	
	            	INeuron in_n = allINeurons.get(in_id);
	            	if(in_n==null){
	            		mlog.say("not found id " + in_id);
	            	}
	            	ProbaWeight p = new ProbaWeight(Constants.fixedConnection);
	            	bw.addStrand(in_n, p);
	            	in_n.addDirectOutWeight(n, bw);
	            	//mlog.say("added strand from " + in_n.getId());
	            	
	            } else if(w_type.equals("proba")) {
	            	int in_id = Integer.valueOf(info[3]);
	            	INeuron in_n = allINeurons.get(in_id);
	            	ProbaWeight p = new ProbaWeight(Constants.defaultConnection);
	            	p.setValue(Integer.valueOf(info[4]));
	            	p.setAge(Integer.valueOf(info[5]));
	            	n.addInWeight(in_n, p);
	            	if(in_n.addOutWeight(n, p)){
	            		//mlog.say("added out weight to " + in_n.getId());
	            	}
	            } else {
	            	mlog.say("*********error");
	            }
	        }
	        br.close();
	        
	        //activate all to be ready for the next timestep
	        for (int i = 0; i < eye_neurons.length; i++) {
				Utils.propagateInstantaneousActivation(eye_neurons[i].values());
			}
	        //for ineurons
			Utils.activateOutWeights(allINeurons);	
			Utils.calculateAndPropagateActivation(allINeurons);
			
		}catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadMemory() {
		
		//prepare network
		//deactivate everything
		cleanAll();
		readingMemory = true;
		try {
			memReader = new BufferedReader(new FileReader(memoryFile));
			String line = memReader.readLine();//skip 1st line
	        mlog.say(line);
	        line = memReader.readLine();//skip 2nd line
	        mlog.say(line);
			//change memory file		
	        memWriter.close();
	        memWriter = new FileWriter(folderName+"/reminiscing_"+Constants.MemoryFileName);
			mlog.say("stream opened "+Constants.MemoryFileName);
        	String str = "iteration,id\n";
        	memWriter.append(str);
        	memWriter.flush();
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
       
	}

	@Override
	public void load(File file, int type) {
		switch (type) {
		case Constants.Net_File_type:
			loadNetwork(file);
			break;
		case Constants.Memory_File_type:
			memoryFile = file;
			loadMemory();
			break;
		default:
			break;
		}
	}

}

