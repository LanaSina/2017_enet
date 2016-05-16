package models;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];//eye_sensors
	
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
			makeNeurons(eye_neurons[i],eyemotor_v);//TODO rename sensor into sensory neurons
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
	 * and all sensors in same layer (will change this when STMem)
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
				//its activation depends on this action
				newn.setAInput(mn.getId());
				//put in action-neurons list to choose where to direct attention
				if(!action_modules.containsKey(mn.getId())){
					ArrayList<INeuron> module = new ArrayList<INeuron>();
					action_modules.put(mn.getId(), module);
					mlog.say("new action module");
				}
				ArrayList<INeuron> module = action_modules.get(mn.getId());//more like motor module
				module.add(newn);
				
				//add outweights to all sensors in all layers
				for(int k=0;k<eye_neurons.length;k++){
					HashMap<Integer, INeuron> layer = eye_neurons[k];
					Iterator it2 = layer.entrySet().iterator();
					while(it2.hasNext()){
						Map.Entry pair2 = (Map.Entry) it2.next();
						INeuron sn2 = (INeuron) pair2.getValue();
						ProbaWeight pw = new ProbaWeight();
						newn.addOutWeight(pw, sn2.id);
						sn2.addInWeight(pw);
					}
				}
							
				//add to reservoir
				allINeurons.put(newn.id,newn);
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
			resetActivation(allINeurons);
			
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
		//TODO buildEyeInput();
	}
	
	
	/**
	 * reset activations of neurons and outweights
	 * of the layer, including actionweights
	 * and actionActivation
	 */
	private void resetActivation(HashMap<Integer, INeuron> layer){
		Iterator it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry) it.next();
			INeuron n = (INeuron) pair.getValue();
			/*n.resetActivation();
			n.resetOutWeightsActivation();
			n.resetActionWeights();*///TODO
		}
	}
	
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
	    		//net.updateCNet();
		
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
