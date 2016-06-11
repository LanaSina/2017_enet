package models;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;


import communication.Constants;
import communication.MyLog;
import neurons.INeuron;
import neurons.ProbaWeight;
import sensors.Eye;

/**
 * 1st stage model: this network just predicts next input using probability weights.
 * Focus cannot change, no action occurs. Has short term memo
 * @author lana
 *
 */
public class SNetSnap {
	
	/** log */
	MyLog mlog = new MyLog("SNet", true);
	
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
	int max_timesteps = 10;
	/** total number of connections in the network (between "hidden" neurons) */
	int n_weights = 0;
	
	
	//environment
	/**images files*/
	String imagesPath = "/Users/lana/Desktop/prgm/JAVANeuron/JAVANeuron/src/images/";
	/** image description (chars)*/
	String[] images = {"1","2","3"};	//{"a","b","c"};		
	
	//sensors w/ actuators
	/** image sensor*/
	Eye eye;
	/** sensory neurons: [layer for this grayscale][id, neuron at different positions in the image] */
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];
	
	//neurons
	/**all neurons except eyes (sensory) so this is like "hidden layer"*/
	HashMap<Integer, INeuron> allINeurons = new HashMap<Integer, INeuron>();
	/** total number of neuron ids*/
	int n_id = 0;
	/**short term memory, contains conscious neurons */
	//HashMap<Integer, INeuron> 
	Vector<INeuron> STM = new Vector<INeuron>();
	
	public SNetSnap(){
    	//sensor init
    	eye = new Eye(imagesPath);
    	mlog.say("read image called");
    	eye.readImage(images[img_id]);
    	
    	//net creation
    	createNet();	
    	//net initialization
    	initNet();
    	
		//main thread
    	new Thread(new ExperimentThread(this)).start();		
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
				//put it in global list
				//allINeurons.put(n_id, n);
				//make it sensitive to an input
				eye.linkNeuron(n_id,j, i);
				n_id++;				
				INeuron n2 = new INeuron(n_id);
				ProbaWeight p = n2.addInWeight(Constants.fixedConnection, n.getId());
				n.addOutWeight(p, n_id);
				n.setUpperNeuron(n2);
				n_weights++;
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
		
		if(nextImage){// && (step<20)
			
    		//mlog.say("presentations "+presentations + " step "+step);
			presentations = 0;
			nextImage = false;
    		//change char
    		img_id++;
			if(img_id>=images.length){
				img_id=0;
			}
			eye.readImage(images[img_id]);
		}
		//build
		buildEyeInput();
	}
	
	
	/**
	 * Resets eye neurons activation to 0;
	 * builds the sensory input from the focused image,
	 * set graphics, 
	 * activates neurons in eye, activate corresponding weights
	 */
	boolean test = false;
	private void buildEyeInput(){	 
		
		//reset activations of eye neurons not their outweights		
		for(int i=0;i<eye_neurons.length;i++){
			resetNeuronsActivation(eye_neurons[i]);
		}
		//reset activations of ineurons
		resetNeuronsActivation(allINeurons);
		
		//apply blur to selected portion of image
		//get grayscale values of the image
		int[] in = eye.buildCoarse(0,0);
		
		//go through sensory neurons and activate them.
		int n = in.length;
		int[][] n_interface = eye.getNeuralInterface();
		for(int k = 0; k<n; k++){
			//values in "in" start at 1, not 0
			int i = in[k]-1;//dont see white -1;
			if(i>0){//dont see white
				eye_neurons[i].get(n_interface[i][k]).increaseActivation(1);
			}
		}//*/
		/*if(test){
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			INeuron n = iterator.next().getValue();
			n.increaseActivation(1);
			mlog.say(n.getId()+" is activated ");
			test = false;
		}else {
			Iterator<Entry<Integer, INeuron>> iterator = eye_neurons[2].entrySet().iterator();
			iterator.next();
			INeuron n = iterator.next().getValue();
			n.increaseActivation(1);
			mlog.say(n.getId()+" is activated ");
			test = true;
		}//*/
		
		//propagate instantly from eye to 1st INeurons
		propagateFromEyeNeurons();
		
		
		//integrate previously predicted activation to actual activation
		//integrateActivation();
		
	}
	

	private void propagateFromEyeNeurons() {
		for (int i = 0; i < eye_neurons.length; i++) {
			Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, INeuron> pair = it.next();
				INeuron n = pair.getValue();
				if(n.getActivation()>0){
					n.activateOutWeights();
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
	 * reset output weights activation to 0 in this layer
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
	
	//main thread
	private class ExperimentThread implements Runnable {
		/** log */
		MyLog mlog = new MyLog("SNet Thread", true);
		/** network */
		SNetSnap net;
		
		public ExperimentThread(SNetSnap net){
			this.net = net;
		}
		
	    public void run() {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  
	
	    	while(true){
	    		long before = 0;
	    		if(step%50==0){
	    			//calculate runtime
	    			before = System.currentTimeMillis();
	    		}
	    		
	    		net.buildInputs();
	    		net.updateSNet();
	    		mlog.say("step " + step);
		
	    		if(step%50==0){
	    			long runtime = System.currentTimeMillis()-before;
	    			//calculate snap time
	    			before = System.currentTimeMillis();
	    			net.snap();
	    			long snaptime = System.currentTimeMillis()-before;;
	    			mlog.say("runtime "+runtime + " snaptime "+ snaptime);
	    		}
	    		
	    		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    		
	    		step++;
	    	}
	    }
	
	}


	/**
	 * update states of all neurons by propagating activation
	 * flow: age output weights, up input weights
	 */
	public void updateSNet() {			

		//update prediction probabilities		
		//add +1 value to the inweights if they were activated at t-1 & neuron is activated
		increaseInWeights();
		//reset activation of all w
		for(int i=0;i<eye_neurons.length;i++){
			resetOutWeights(eye_neurons[i]);
		}		
		resetOutWeights(allINeurons);
		
		//age output weights of currently activated neurons	in INeurons
		ageOutWeights(allINeurons);	
		
		//activate weights from sensory neurons		
		/*for(int i=0;i<eye_neurons.length;i++){
			activateOutWeights(eye_neurons[i]);
		}*/
		//for ineurons
		activateOutWeights(allINeurons);
		
		//recalculate predicted activations and
		//look at predictions
		//buildPredictionMap();
		
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
			if(n.isActivated()){
				//mlog.say("activated");
				STM.add(n);
			}
		}
		
		mlog.say("stm "+ STM.size());
	}

	/**
	 * create weights from previously conscious neurons to current surprised neurons.
	 * In this model there is no topological hierarchy yet, so all activated neurons are conscious.
	 */
	private void makeWeights() {
		
		//ineurons 
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		int nw = 0;
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.calculateActivation();
			if(n.isSurprised()){
				mlog.say("is surprised");
				//go through STM
				for (Iterator<INeuron> iterator = STM.iterator(); iterator.hasNext();) {
					INeuron preneuron = iterator.next();
					//doubloons weights will not be added
					ProbaWeight probaWeight = n.addInWeight(Constants.defaultConnection, preneuron.getId());
					if(preneuron.addOutWeight(probaWeight, n.getId())){
						nw++;
						n_weights++;
						//mlog.say(preneuron.getId()+" to "+n.getId());
					}
				}
			}
		}
		mlog.say("added " + nw + " weights");
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
				//neuron.receiveUpperPredictedActivation();
				if(neuron.getUpperPredictedActivation()>0){
					//mlog.say(neuron.getId()+" inweight was activated ");
					sum[j]=sum[j]+1;
					//don't take contradictions into consideration for now (we don't have actions, so no contradictions will happen)
					//if white, dont't add anything
					// gray
					coarse[j] = coarse[j] + i;//trying to get stronger values for higher scales.
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
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.getActivation()>0){
				n.ageOutWeights();
			}
		}
	}
	
	/**
	 * age the outweights of neurons that are currently activated
	 */
	private void increaseInWeights() {
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.getActivation()>0){
				n.increaseInWeights();
			}
		}
	}

	
	/** fuses something
	 * TODO before this: create a layer of hidden neurons linked to eye neurons;
	 * remove eyes from allINeurons
	 * add fixed weights from eye neurons to i neurons.
	 * */
	private void snap() {
		mlog.say("snapping");
		mlog.say("total connections "+n_weights);
		ArrayList<Integer> remove = new ArrayList<Integer>();
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
						//compare all out weights
						//& !n2.canLearn() & 
						double diff = 0;
						int all = 0;
						HashMap<Integer,ProbaWeight> out1 = n.getOutWeights();
						HashMap<Integer,ProbaWeight> out2 = n2.getOutWeights();
						Iterator<Entry<Integer, ProbaWeight>> out1it = out1.entrySet().iterator();
						while(out1it.hasNext()){
							Map.Entry<Integer, ProbaWeight> out1pair = out1it.next();
							ProbaWeight w = out1pair.getValue();	
							//does n2 have all the same outweights?
							Iterator<Entry<Integer, ProbaWeight>> out2it = out2.entrySet().iterator();
							if(!out2.containsKey(out1pair.getKey())){
								//give up on this n2
								break;//TODO labeled break might be necessary
							} else {
								//contains weight to same neuron; check value
								ProbaWeight w2 = out2.get(out1pair.getKey());
								diff += Math.pow(w.getProba() - w2.getProba(),2);
								all++;
							}
							
							double dist = Math.sqrt(diff)/all;
							if(dist==0){//exact same outweights
								boolean dosnap = true;
								//check if no direct contradiction in inweights
								HashMap<Integer,ProbaWeight> in = n.getInWeights();
								HashMap<Integer,ProbaWeight> in2 = n2.getInWeights();
								Iterator<Entry<Integer, ProbaWeight>> in1it = out1.entrySet().iterator();
								while(in1it.hasNext()){
									Map.Entry<Integer, ProbaWeight> in1pair = in1it.next();
									ProbaWeight inw = in1pair.getValue();	
									Iterator<Entry<Integer, ProbaWeight>> in2it = in2.entrySet().iterator();
									if(in2.containsKey(in1pair.getKey())){
										//check that they do not individually contradict
										ProbaWeight inw2 = in2.get(in1pair.getKey());
										double indiff = Math.pow(inw.getProba() - inw2.getProba(),2);
										if(indiff>0.01){
											dosnap = false;//give up
										}
									}
								}
								if(dosnap){
									n.justSnapped = true;
									n2.justSnapped = true;
									//report n2 inputs to n if they did not exist
									Iterator<Entry<Integer, ProbaWeight>> in2it = in2.entrySet().iterator();
									//number of inweights not deleted
									int nin = 0;
									while(in2it.hasNext()){
										Map.Entry<Integer, ProbaWeight> in2pair = in2it.next();
										if(n.addInWeight(in2pair)){
											nin++;
										}
									}
									//do the same for direct inweights
									HashMap<Integer,ProbaWeight> din = n2.getDirectInWeights();
									in2it = din.entrySet().iterator();
									while(in2it.hasNext()){
										Map.Entry<Integer, ProbaWeight> in2pair = in2it.next();
										if(n.addDirectInWeight(in2pair)){
											nin++;
										}
									}
									
									//n2 will be deleted after this
									remove.add(n2.getId());
									n_weights-=out2.size();//all the outweights of n2
									n_weights = n_weights-in2.size()+nin;
								}
							}
						}
					}
				}
			}
		}
		for(int i=0; i<remove.size();i++){
			allINeurons.remove(remove.get(i));
		}
		
		//reset "just snapped" values
		it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.justSnapped = false;
		}
		
		mlog.say("after "+ allINeurons.size());
	}
}

