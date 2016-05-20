package models;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import communication.Constants;
import communication.MyLog;
import neurons.INeuron;
import neurons.MotorNeuron;
import neurons.ProbaWeight;
import sensors.Eye;


/**
 * Rebuilding the whole project in a cleaner way.
 * @author lana
 *
 */
public class CharSNet {
	/** log */
	MyLog mlog = new MyLog("CarCNet", true);
	
	/**number of presentations for current image*/
	int presentations = 0;
	/**number of timesteps we train on each image*/
	int max_presentations = 10;
	/**Go to the next image*/
	boolean nextImage = false;
	/**current image*/
	int img_id = 0;
	
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
	
	//actuators
	//motion chosen by the network
	int h_m;
	int v_m;
	//motor modules: (one per muscle, so here instead of human 4 muscles
	//we have only 2 muscles that can be at rest (center), right, of left.
	/** horizontal motion muscle */
	ArrayList<MotorNeuron> eyemotor_h = new ArrayList<MotorNeuron>();
	/** vertical motion muscle */
	ArrayList<MotorNeuron> eyemotor_v = new ArrayList<MotorNeuron>();
	
	
	public CharSNet(){
    	//sensor init
    	eye = new Eye(imagesPath);
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
		
		
		//motor neurons
		//move right or left 
		for(int i=0; i< eye.getHorizontalMotionResolution();i++){	
			//this neuron links to this action
			MotorNeuron m = new MotorNeuron(n_id);
			eyemotor_h.add(m);	
			n_id++;
		}	
		//up or down
		for(int i=0; i< eye.getVerticalMotionResolution();i++){	
			//neuron links to this action
			MotorNeuron m = new MotorNeuron(n_id);
			eyemotor_v.add(m);	
			n_id++;
		}	
	
		//build hidden neurons with their weights
		for(int i=0;i<eye_neurons.length;i++){
			makeNeurons(eye_neurons[i],eyemotor_v);
			makeNeurons(eye_neurons[i],eyemotor_h);
		}	
		
		//auditory neurons (a,b,c)
		//have no pweights for now
		/*for(int i = 0; i<images.length;i++){
			CMotorNeuron mn = new CMotorNeuron(n_id);
			n_id++;
			hear.add(mn);
		}*/
		
		mlog.say(n_id +" neurons");		
	}
	
	
	
	/**
	 * make neurons taking input from each sensor, and of which activation depends on each motor neuron in the layer.
	 * One hidden neuron per eye sensor, per motor neuron, with weights towards all actions
	 * and all sensors in same layer (will change this when we have STMem)
	 * @param sensory layer
	 * @param motor layer
	 */
	private void makeNeurons(HashMap<Integer, INeuron> sensory, ArrayList<MotorNeuron> motor){
		//go through sensory
		Iterator it = sensory.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
			INeuron sn = (INeuron) pair.getValue();
			//go through motor (which action will activate this neuron?)
			for(int i=0; i<motor.size();i++){
				MotorNeuron mn = motor.get(i);
				//create new neuron
				INeuron newn = new INeuron(n_id);
				n_id++;
				//add weight from eye sensor to this neuron
				ProbaWeight p = newn.addInWeight(Constants.defaultConnection, sn.getId());//was RealInWeight
				sn.addOutWeight(p,newn.getId());					
				//its activation depends on this action (disappears when we have neuron grouping)
				
				//add outweights to all sensors in all "layers"
				for(int k=0;k<eye_neurons.length;k++){
					HashMap<Integer, INeuron> layer = eye_neurons[k];
					Iterator it2 = layer.entrySet().iterator();
					while(it2.hasNext()){
						Map.Entry pair2 = (Map.Entry) it2.next();
						INeuron sn2 = (INeuron) pair2.getValue();
						ProbaWeight pw = sn2.addInWeight(Constants.defaultConnection, sn2.getId());
						newn.addOutWeight(pw, sn2.getId());
					}
				}							
				//add to reservoir
				allINeurons.put(newn.getId(),newn);
			}
		}	
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
			//deactivate the previous inweights and "action activations"
			//resetActivation(allINeurons); //not conviced this is necessary.
			
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
		int[] in = eye.buildCoarse(v_m,h_m);
		
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
		//resetActivation(allINeurons);//mmh...
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
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
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
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
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
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetOutWeights();
		}
	}
	
	
	/**
	 * increases the value of input weights in the layer
	 * @param layer
	 */
	private void increaseInWeights(HashMap<Integer, INeuron> layer){
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
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
	
	
	/**
	 * reset activations of neurons and outweights
	 * of the layer, including actionweights
	 * and actionActivation
	 */
	/*private void resetActivation(HashMap<Integer, INeuron> layer){
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
			INeuron n = (INeuron) pair.getValue();
			/*n.resetActivation();
			n.resetOutWeightsActivation();
			n.resetActionWeights();*///todo 
	/*	}
	}*/
	
	//main thread
	private class ExperimentThread implements Runnable {
		/** frequency at which to snap the network */
		int step = 0;
		/** log */
		MyLog mlog = new MyLog("FocusNet Thread", true);
		/** network */
		CharSNet net;
		
		public ExperimentThread(CharSNet net){
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
	    		//TODO net.updateSNet();
		
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
		
}
