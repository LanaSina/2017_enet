package models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import communication.Constants;
import communication.MyLog;
import neurons.INeuron;
import sensors.Eye;

//TODO draw activation map (!= simple input map)
//TODO draw prediction map

/**
 * 1st stage model: this network just predicts next input using probability weights.
 * Focus cannot change, no action occurs. Has short term memo
 * @author lana
 *
 */
public class SNetSnap {
	
	/** log */
	MyLog mlog = new MyLog("CarCNet", true);
	
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
	
	//environment
	/**images files*/
	String imagesPath = "/Users/lana/Desktop/prgm/JAVANeuron/JAVANeuron/src/images/";
	/** image description (chars)*/
	String[] images = {"a","b","c"};		
	
	//sensors w/ actuators
	/** image sensor*/
	Eye eye;
	/** sensory neurons */
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];//was eye_sensors
	
	//neurons
	/**all neurons except eyes (sensory) so this is like "hidden layer"*/
	HashMap<Integer, INeuron> allINeurons = new HashMap<Integer, INeuron>();
	/** total number of neuron ids*/
	int n_id = 0;
	
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
		
		//eye sensory neurons. all neurons must have different ids
		for(int i=0; i< n_n;i++){				
			for(int j=0;j<gray_scales;j++){
				//create neuron
				INeuron n = new INeuron(n_id);
				//put it in list
				eye_neurons[j].put(n_id, n);
				//make it sensitive to an input
				eye.linkNeuron(n_id,j, i);				
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
			
    		mlog.say("presentations "+presentations);
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
	private void buildEyeInput(){	 
		
		//reset activations of eye neurons, but NOT their out weights		
		for(int i=0;i<eye_neurons.length;i++){
			resetNeuronsActivation(eye_neurons[i]);
		}
		
		//apply blur to selected portion of image
		int[] in = eye.buildCoarse(0,0);
		
		//go through sensory neurons and activate them.
		int n = in.length;
		int[][] n_interface = eye.getNeuralInterface();
		for(int k = 0; k<n; k++){
			//values in "in" start at 1, not 0
			int i = in[k]-1;
			eye_neurons[i].get(n_interface[i][k]).increaseActivation(1);//actually, activation could just be binary
		}
				
		//update prediction probabilities: inweight
		for(int i=0;i<eye_neurons.length;i++){
			//add +1 value to the inweights if they were activated at t-1 
			//& if this is activated
			increaseInWeights(eye_neurons[i]);
		}
		//reset activations of out weights
		//must be separate from above
		for(int i=0;i<eye_neurons.length;i++){
			resetOutWeights(eye_neurons[i]);
		}

		//now it's ok to deactivate all inweights (they're outweights from t-1)
		resetInWeights(allINeurons);
		
		//activate outweights for use at t+1
		for(int i=0;i<eye_neurons.length;i++){
			//activate weights from sensory neurons		
			activateOutWeights(eye_neurons[i]);
		}			
	}
	
	
	/**
	 * Reset in weights to 0 in this layer
	 * @param layer
	 */
	private void resetInWeights(HashMap<Integer, INeuron> layer) {
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetInWeights();
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
	 * increases the value of input weights in the layer
	 * @param layer
	 */
	private void increaseInWeights(HashMap<Integer, INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			if(ne.isActivated()){
				ne.increaseInWeights();
			}
		}
	}
	
	
	/**
	 * resets neurons activation to 0
	 * @param layer map of neurons to be reset.
	 */
	public void resetNeuronsActivation(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Entry<Integer, INeuron> pair = it.next(); //this way of writing supresses warning
			//Map.Entry pair = (Map.Entry) it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetActivation();
		}
	}
	
	//main thread
	private class ExperimentThread implements Runnable {
		/** frequency at which to snap the network */
		int step = 0;
		/** log */
		MyLog mlog = new MyLog("FocusNet Thread", true);
		/** network */
		SNetSnap net;
		
		public ExperimentThread(SNetSnap net){
			this.net = net;
		}
		
	    public void run() {
	
	    	while(true){
	    		long before = 0;
	    		if(step%50==0){
	    			//calculate runtime
	    			before = System.currentTimeMillis();
	    		}
	    		
	    		net.buildInputs();
	    		net.updateSNet();
		
	    		if(step%50==0){
	    			long runtime = System.currentTimeMillis()-before;
	    			//calculate snap time
	    			before = System.currentTimeMillis();
	    			//net.cSnap(); do this when there is a clean consciousness
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
	 */
	public void updateSNet() {		
		//activate hidden neurons	
		//propagate activation of proba weights from hidden neurons
		propagateHiddenActivation();
			
		//check for discrepancies
		//TODO checkPredictions();
		
		//TODO ageHiddenWeights();
		//do the surprise here?
	
		//inputs haven't been set to 0 yet so check who's brought up by 
		//TODO updateConsciousList();	
		
		//input activations are reset at the beginning of next step.
	}

	
	/**
	 * calculates in activation for everyone
	 * and activates out weights.
	 */
	private void propagateHiddenActivation(){
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.activateOutWeights();
		}
	}
}
