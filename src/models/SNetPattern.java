package models;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
	boolean save = false;
	
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
	//
	int activated = 0;
	

	//environment
	/**images files*/
	String imagesPath = "/Users/lana/Desktop/prgm/SNet/images/ball/"; 
	/** leading zeros*/
	String name_format = "%02d";
	/** number of images if not using names*/
	int n_images = 1;//Constants.n_images;
	
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
	
	
	//actions
	/**vector of ids of activated muscles (id must correspond to motion value in Eye)*/
	Vector<Integer> h_muscles = new Vector<Integer>();
	/** vector of ids of activated muscles (id must correspond to motion value in Eye)*/
	Vector<Integer> v_muscles = new Vector<Integer>();
	//proprioception
	/** proprioceptive neurons (horizontal eye muscle)*/
	ArrayList<INeuron> eyepro_h = new ArrayList<INeuron>();
	/** proprioceptive neurons (vertical eye muscle)*/
	ArrayList<INeuron> eyepro_v = new ArrayList<INeuron>();
	/** id range of propriocetptive INeurons \*/
	int pi_start = 0;
	int pi_end = 0;

	
	public SNetPattern(){
		//graphics
		//todo read this from constants file
    	panel = new Surface();
    	panel.addControllable(this);   
		
    	//sensor init
    	eye = new Eye(imagesPath,panel);
    	//leading zeros
		String iname =  String.format(name_format, img_id); //images[img_id];//"%010d"
    	eye.readImage(iname);
    	
    	//net creation
    	createNet();	
    	//net initialization
    	initNet();
    	
    	//init potential folder name
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    folderName = Constants.DataPath + "/" + strDate + "/";
    	if(save){
    		initDataFiles();
    	}
    	
    	//graphics
    	if(draw_net){
	    	netGraph = new NetworkGraph((HashMap<Integer, INeuron>) allINeurons.clone(), eye_neurons, eye);
		    netGraph.show(); 
	    	netGraph.setName(strDate);
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
        			"," + eye.has_noise + "," + eye.noise_rate + "," + eye.noise_rate + ",max_layers,"+
        			+ Constants.eres_f + "," + Constants.eres_nf + "," + Constants.gray_scales +","+ /*max_new_connections*/ "infinite" + "," + /*max_in_weights*/ "infinite" + "\n";
        	param_writer.flush();
        	param_writer.close();
        	
			//parameters
			net_param_writer = new FileWriter(folderName+"/"+Constants.Net_param_file_name);
			mlog.say("stream opened "+Constants.Net_param_file_name);
        	str = "iteration,neurons,connections\n";
        	net_param_writer.append(str);
        	net_param_writer.flush();
        	
        	//surprise 
        	perfWriter = new FileWriter(folderName+"/"+Constants.PerfFileName);
			mlog.say("stream opened "+Constants.PerfFileName);
        	str = "iteration,surprise\n";
        	perfWriter.append(str);
        	perfWriter.flush();
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
	
	private void writeSurprise(double n) {
		String str = step+","+ n +"\n";
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
				double[] p = {sensor_i,sensor_j,0,0};
				n.setPosition(p);
				//make it sensitive to an input
				eye.linkNeuron(n_id,j, i);
				n_id++;				
				INeuron n2 = new INeuron(n_id);
				//n2.justSnapped = true;//avoid snapping newborn neurons
				//add direct in weight
				Vector<INeuron> v = new Vector<INeuron>();
				v.addElement(n);
				BundleWeight b = n2.addDirectInWeight(v);
				n.addDirectOutWeight(n2,b);
				allINeurons.put(n_id, n2);
				n_id++;				
			}
		}	
		si_end = n_id-1;

		mlog.say("Initial INeurons: id "+ si_start + " to "+ si_end);
		
		pi_start = n_id;
		//move right or left 
		for(int i=0; i< eye.getHorizontalMotionResolution();i++){	
			INeuron n = new INeuron(n_id);
			double[] p = {-i,0,0,0};
			n.setPosition(p);
			eyepro_h.add(n);	
			allINeurons.put(n.getId(), n);
			//mlog.say("Proprioception INeuron " + n_id);
			n_id++;			
		}	
		
		//up or down
		for(int i=0; i< eye.getVerticalMotionResolution();i++){	
			INeuron n = new INeuron(n_id);
			double[] p = {-i,1,0,0};
			n.setPosition(p);
			eyepro_v.add(n);	
			allINeurons.put(n.getId(), n);
			//mlog.say("Proprioception INeuron " + n_id);
			n_id++;
		}
		pi_end = n_id-1;
		
		mlog.say("Proprioception INeurons: id "+ pi_start + " to "+ pi_end);
		
		mlog.say(n_id +" neurons");		
		
		resetNeuronsActivation(allINeurons);
	}
	
	
	/**
	 * resets all neurons activations,
	 * then builds the sensory inputs 
	 * and activate all outside weights and action_weights accordingly.
	 */
	public void buildInputs(){
		
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
		
		//choose actions, activate "proprioceptive" neurons, act at next step
		findActions();
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
				if(i>=0){//>=0 if seeing white
					eye_neurons[i].get(n_interface[i][k]).increaseActivation(1);
				}
			}//*/
		}else{
			//make dreams: activate 60 sensors at random (total number of non overlapping sensors = 184
			/*int[][] n_interface = eye.getNeuralInterface();
			for(int i=0; i<60; i++){
				//four layers of sensors (greyscale)
				int l =  (int) Constants.uniformDouble(0,4);//0..3
				//184 positions
				int p =  (int) Constants.uniformDouble(0,184);//0..3
				eye_neurons[l].get(n_interface[l][p]).increaseActivation(1);			
			}*/
			
			if(step%10==0){
				//unactivate all neurons (just in case we are overloaded)
				deactivateAll();
				activated = 0;
			}
			
			Object[] neurons = allINeurons.values().toArray();
			int max = neurons.length;
			int total = (max/20) - activated;//don't overload net
			mlog.say("total "+ total + " activated "+ activated);
			for(int i=0; i<total; i++){
				int l =  (int) Constants.uniformDouble(0,max);
				((INeuron) neurons[l]).increaseActivation(1);
			}
			
		}
		
		//proprioception here works before actual contraction
		//useful bc behaviour is random, but in the future proprioception can happen
		//during motion as normal
		//send order to eye
		int[] proprio = eye.contractMuscles(v_muscles, h_muscles);
		int v_m = proprio[0];
		int h_m = proprio[1];

		INeuron np = eyepro_v.get(v_m+1);
		np.increaseActivation(1);
		np = eyepro_h.get(h_m+1);
		np.increaseActivation(1);
		mlog.say("intention "+np.getId());

		String action = "h "+ h_m + " v " + v_m;
		panel.setAction(action);

		//propagate instantly from eyes
		for (int i = 0; i < eye_neurons.length; i++) {
			Utils.propagateInstantaneousActivation(eye_neurons[i].values());
		}
		Utils.propagateInstantaneousActivation(eyepro_h);
		Utils.propagateInstantaneousActivation(eyepro_v);

		//integrate previously predicted activation to actual activation
		if(dreaming){
			Iterator<INeuron> it = allINeurons.values().iterator();
			while(it.hasNext()){
				INeuron n  = it.next();
				if(n.getActivation()>0){
					n.activateDirectOutWeights();
				}			
			}	
			integrateActivation();	
			activated = getActivated();
		}
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

		//update prediction probabilities		
		Utils.ageOutWeights(allINeurons);
		Utils.increaseInWeights(allINeurons);

		//reset activation of all w
		for(int i=0;i<eye_neurons.length;i++){
			Utils.resetOutWeights(eye_neurons[i]);
		}		
		Utils.resetOutWeights(allINeurons);
		
		//for ineurons
		Utils.activateOutWeights(allINeurons);	
		//muting happens here
		//predicted activation for next step calculated here
		Utils.calculateAndPropagateActivation(allINeurons);
		
		if(testp != null){
			mlog.say("+++++++ activation "+	testp.getActivation());
		}
		//create new weights based on (+) surprise
		makeWeights();
		
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
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			//no hierarchy: all activated neurons are remembered, including sensory neurons.
			//if(n.isMute()) mlog.say("muted "+ n.getId());
			if(n.isActivated() & !n.isMute()){
				STM.add(n);
				if(n.getId()>=2208 && n.getId()<2213){
					mlog.say("000000 STM includes motion neuron");
				}
				if(n.getId()>2213){
					mlog.say("1111111 STM includes pattern neuron");
				}
			}
		}
		
		//mlog.say("stm "+ STM.size());
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
		
		//number of surprised neurons at this timestep
		int n_surprised = 0;
		//number of sensory activates
		int n_activated = 0;
		//predicted, not activated
		int n_illusion = 0;
		//(maybe we should make this for all, not sensory neurons)
		//suprise = suprised sensory neurons / number of activated sensory neurons
		
		if(STM.size()==0){
			mlog.say("just woke up");
			if(save){
				writeSurprise(0);
			}
			return;
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
				int id = n.getId();
				//do not try to predict proprioception: action choice is random for now
				if(id>=pi_start && id<=pi_end){
					continue;
				}
	
				if(id>=si_start && id<=si_end){
					if(n.isActivated()){
						n_activated++;
					} else {
						double pr = n.getPredictedActivation();
						//false positive
						if(pr>0){
							n_illusion++;
						}
					}
				}				
				
				if(n.isSurprised()){// && !n.isMute() must predict activation of small ones too
					mlog.say("+++++++++ " + n.getId() + " surprised ");
					if(id>=si_start && id<=si_end){
						n_surprised++;
					}
					//did we improve future prediction chances?
					boolean didChange = false;
					
					//go through STM
					for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
						INeuron preneuron = iterator.next();
						
						if((cpu_limitations && nw>max_new_connections)) break;
						
						//doubloons weights will not be added
						ProbaWeight probaWeight = n.addInWeight(Constants.defaultConnection, preneuron);
						if(preneuron.addOutWeight(n,probaWeight)){
							probaWeight.setActivation(1,null);
							nw++;
							didChange = true;
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
							
						//no change happened, try building a spatial pattern
						if(!didChange && !dreaming){	// 
							if(cpu_limitations && nw>max_new_connections) break;
							
							if(!hasMaxLayer(STM)){
								Vector<INeuron> vn = Utils.patternExists3D(STM, n);
								if(vn.size()>0){
									if(the_pattern==null){
										if(vn.size()>1){
											mlog.say("******** added pattern neuron id "+ n_id + " to " + n.getId());
											
											if(vn.get(vn.size()-1).getId()>2213){
												mlog.say("***** countains pattern neuron");
											}
											for (Iterator<INeuron> iterator2 = vn.iterator(); iterator2.hasNext();) {
												INeuron iNeuron = (INeuron) iterator2.next();
												mlog.say("ID "+ iNeuron.getId());
												if(iNeuron.getId()>=2208 && iNeuron.getId()<2213){
													mlog.say("--------- pattern neuron includes motion neuron "+iNeuron.getId());
												}
											}
											
											the_pattern = new INeuron(vn,n,n_id);
											n_id++;
											newn.addElement(the_pattern);
											ProbaWeight weight = the_pattern.getOutWeights().get(n);
											weight.setActivation(1, null);
										}else{
											//only used when debugging
											INeuron pn = vn.get(0);
											ProbaWeight p = n.addInWeight(Constants.defaultConnection, pn);
											pn.addOutWeight(n, p);
											p.setActivation(1, null);
											mlog.say("******** added p weight from "+ pn.getId() + " to " + n.getId());
										}
										
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
		if(save){		
			double perf = (n_surprised*1.0/n_activated) //false negatives
					+ (n_illusion*1.0/n_activated); //false positives
			writeSurprise(perf);
		}
		mlog.say(" surprised: " + n_surprised + " illusions " + n_illusion);

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
		allINeurons = Utils.snap(allINeurons);
	}
	
	private void cleanAll() {
		STM.clear();
		deactivateAll();
	}
	
	
	/**
	 * decide of the next action to do
	 */
	private void findActions() {
		//array of "intentions"
		//in the end, actions will be "flavored" to allow choice
		//ArrayList<Integer> actionsID = new ArrayList<Integer>();//dirty	
		
		//random eye actions
		h_muscles.clear();
		int act =  (int) Constants.uniformDouble(0,3);//0..2
		h_muscles.addElement(act);
		
		v_muscles.clear();
		act =  (int) Constants.uniformDouble(0,3);
		v_muscles.addElement(act);
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
		    		if(step%Constants.snap_freq==0){
		    			//calculate runtime
		    			before = System.currentTimeMillis();
		    		}
		    		
		    		//also unmutes neurons
		    		net.buildInputs();
  
		    		net.updateSNet();
		    		int nw = Utils.countWeights(allINeurons);
		    		mlog.say("step " + step +" weights "+nw);
			
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
		    			
		    			//sleep for 20 steps, every 20 steps
		    			/*if(step>1){
			    			if(!dreaming){
			    				dreaming = true;
			    				cleanAll();
			    				mlog.say("dreaming");
			    			}else{	    				
			    				dreaming = false;
			    				cleanAll();
			    				mlog.say("not dreaming");
			    			}
		    			}*/
		    		}
		    		
		    		
		    		step++;
		    		
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
		writeWeights();
		//reactivate button
		saveButton.setEnabled(true);	
	}

	@Override
	public void refresh() {
		if(draw_net){
			netGraph.redraw();
		}
	}

}

