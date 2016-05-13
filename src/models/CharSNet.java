package models;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import communication.Constants;
import communication.MyLog;
import graphics.Surface;
import neurons.INeuron;
import sensors.Eye;
import testNets.CMotorNeuron;
import testNets.CNeuron;

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
	
	
	//sensors
	/** image sensor*/
	Eye eye;
	/** sensory neurons */
	HashMap<Integer, INeuron>[] eye_neurons = new HashMap[Constants.gray_scales];//eye_sensors
	
	//neurons
	/**all neurons except eyes (sensory) so this is like "hidden layer"*/
	HashMap<Integer, INeuron> allINeurons = new HashMap<Integer, INeuron>();
	/** total number of neuron ids*/
	int n_id = 0;
	
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
		//fill interfaces
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
		
		//interface: fields
		int n = 0;
		int w = 0;
		int h = 0;
		//do in focus first,left to right
		h = (vf_h-ef_s)/2;
		w = (vf_w-ef_s)/2;
		boolean next = true;
		while(next){
			eye_interface[n][0] = h;//row
			eye_interface[n][1] = w;//col
			eye_interface[n][2] = eres_f;//size
			w+=eres_f;//next column
			if(w >= ((vf_w-ef_s)/2)+ef_s){
				//next row
				h+=eres_f;
				w=(vf_w-ef_s)/2;
			}		
			if(h >= ((vf_h-ef_s)/2)+ef_s){
				next = false;
			}
			n++;
		}
	
		//now do outfocus
		h = 0;
		w = 0;
		next = true;
		while(next){
			boolean infocus = ( h>=(vf_h-ef_s)/2 & h<((vf_h-ef_s)/2)+ef_s) & (w>=(vf_w-ef_s)/2 & w<((vf_w-ef_s)/2)+ef_s);
			if(!infocus){
				eye_interface[n][0] = h;//row
				eye_interface[n][1] = w;
				eye_interface[n][2] = eres_nf;//size
				w+=eres_nf;//next column
				n++;
			} else{
				w+=ef_s;
			}
			if(w >= vf_w){
				//next row
				h+=eres_nf;
				w=0;
			}				
			if(h >= vf_h){
				next = false;
			}
		}
		
		mlog.say("interface n "+ n);
		
		//motor layers of neurons
		//move right or move left 
		for(int i=0; i< eyemuscle_h.length;i++){	
			//this neuron links to this action
			CMotorNeuron m = new CMotorNeuron(n_id);
			eyemotor_h.add(m);	
			n_id++;
		}	
		//up or down
		for(int i=0; i< eyemuscle_v.length;i++){	
			//this neuron links to this action
			CMotorNeuron m = new CMotorNeuron(n_id);
			eyemotor_v.add(m);	
			n_id++;
		}	
	
		//build hidden neurons with their weights
		for(int i=0;i<eye_sensors.length;i++){
			makeNeurons(eye_sensors[i],eyemotor_v);//TODO rename sensor into sensory neurons
			makeNeurons(eye_sensors[i],eyemotor_h);
		}	
		
		//auditory neurons (a,b,c)
		//have no pweights for now
		for(int i = 0; i<images.length;i++){
			CMotorNeuron mn = new CMotorNeuron(n_id);
			n_id++;
			hear.add(mn);
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
			bw = eye.readImage(images[img_id]);
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
