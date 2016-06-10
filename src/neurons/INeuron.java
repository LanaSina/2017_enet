package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import communication.Constants;
import communication.MyLog;

/**
 * Equivalent to previous "CNeuron".
 * Intermediate neuron between sensors (sensory neurons) and actuators (action neurons).
 * Can be snapped.
 * 
 * @author lana
 *
 */
public class INeuron extends Neuron {
	MyLog mlog = new MyLog("INeuron", true);

	HashMap<Integer, ProbaWeight> inWeights = new HashMap<Integer, ProbaWeight>();//was realInWeights
	/** (id of out neuron, weight) probabilistic outweights*/
	HashMap<Integer, ProbaWeight> outWeights = new HashMap<Integer, ProbaWeight>();
	/** activation of this neuron (real or vitual)*/
	double activation;
	/** probabilistic activation */
	double pro_activation;
	/** has activation been calculated since the last reset or not*/
	boolean activationCalculated = false;
	/** predicted activation */
	int predictedActivation;
	/** has activation been calculated since the last reset or not*/
	boolean predictedActCalculated = false;
	/** does the prediction corresponds to any kind of input*/
	boolean surprised = false;
	/** used when pruning neurons*/
	public boolean justSnapped = false;
	
	public INeuron(int id) {
		super(id);
	}


	/**
	 * Adds a neuron as input to this.
	 * 
	 * @param wtype type of the input connection
	 * @param id id of the input neuron
	 * @return existing or newly created connection.
	 */
	public ProbaWeight addInWeight(int wtype, int id) {		
		ProbaWeight p;
		if(inWeights.containsKey(id)){
			p = inWeights.get(id);
		}else{
			p = new ProbaWeight(wtype);
			inWeights.put(id, p);
		}
		return p;
	}


	/**
	 * takes the inweight of a neuron and makes this neuron as input.
	 * does not add the connection if similar connection exists
	 * @param p weight
	 * @param id id of the output neuron
	 * @return true if the weight was added, false if it already existed
	 */
	public boolean addOutWeight(ProbaWeight p, int id) {
		boolean b = false;
		if(outWeights.containsValue(p)){
			//mlog.say("addOutWeight: already exists");
		}else{
			outWeights.put(id, p);
			b = true;
		}		
		return b;
	}
	
	
	/**
	 * sets activation of the neuron to 0
	 * does not change activation of output weights.
	 */
	public void  resetActivation(){
		activation = 0;
		activationCalculated = false;
	}

	
	/**
	 * increases neuron activation
	 * should not be used outside of direct connection to sensors
	 * @param i value to add
	 */
	public void increaseActivation(int i) {
		activation = activation+i;
		activationCalculated = false;
	}

	/**
	 * increase the value of the in weights that were activated 
	 * and young enough to still be learning
	 * (not the age)
	 */
	public void increaseInWeights(){
		Iterator<Entry<Integer, ProbaWeight>> it = inWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ProbaWeight> pair = it.next();
			ProbaWeight w = (ProbaWeight) pair.getValue();
			//value is increased if this weight was previously activated
			if(w.canLearn & w.isActivated()){//w.getWasActivated() & 
				w.addValue();
			}
		}
	}

	/**
	 * recalculates activation
	 * @return true is activation is positive, false otherwise
	 */
	public boolean isActivated() {
		//Calculate activation once only
		//calculateActivation();	
		
		boolean b = false;
		if(activation>0){
			b = true;
		}
		return b;
	}


	/**
	 * calculates the probabilistic activation of this neuron
	 * and compares it to its direct activation.
	 * if there is not predicted activation but we are activated, the neuron should be "surprised"*/
	//TODO surprise: no input is not same as 0 input
	public void calculateActivation() {
		if(!activationCalculated){
			surprised = false;
			//calculate predicted activation a
			double a = 0;
			double confidence = Constants.confidence_threshold;
			
			Iterator<Entry<Integer, ProbaWeight>> it = inWeights.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, ProbaWeight> pair = it.next();
				ProbaWeight pw = pair.getValue();
				double w  = pw.getProba();
				//mlog.say(id+ " w "+w);
				if(w>confidence & pw.isActivated()){
					a+=1;
					//mlog.say(" was activated from "+pair.getKey()+" to "+id);
				}
			}
	
			//mlog.say("predicted "+a+ " real "+ activation);
			if(pro_activation==0 & activation>0){//(a==0 & activation>0){
				surprised = true;
				//mlog.say("surprised");
			} else if(a>0){
				//mlog.say("prediction exists");
			}
		
			pro_activation = a;
			activationCalculated = true;
		}
	}


	/**
	 * reset output weights activation to 0.
	 */
	public void resetOutWeights() {
		Iterator<Entry<Integer, ProbaWeight>> it = inWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ProbaWeight> pair = it.next();
			ProbaWeight w = (ProbaWeight) pair.getValue();
			w.resetActivation();
		}		
	}

	
	/**
	 * set the activation values to all outside weights
	 * based on the activation of this neuron
	 */
	public void activateOutWeights() {
		sendActivations();
	}
	
	
	/**
	 * sets activations of all outside probabilistic weights
	 * to 1 if activated
	 */
	private void sendActivations(){
		if(!isActivated()){
			return;
		}
		Iterator<Entry<Integer, ProbaWeight>> it = outWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.setActivation(1);
		}			
	}


	/**
	 * set all in weights activations to 0
	 */
	public void resetInWeights() {
		Iterator<Entry<Integer, ProbaWeight>> it = inWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.resetActivation();
		}			
	}


	/**
	 * 
	 * @return output weights of this neuron
	 */
	public HashMap<Integer, ProbaWeight>  getOutWeights() {
		return (HashMap<Integer, ProbaWeight>) outWeights.clone();
	}


	public void ageOutWeights() {
		Iterator<Entry<Integer, ProbaWeight>> it = outWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ProbaWeight> pair = it.next();
			ProbaWeight w = pair.getValue();
			w.increaseAge();
		}			
	}


	/**
	 * call this before integrating predictions!
	 * (cpu intensive. call once or store value)
	 */
	public double getPredictedActivation() {		
		return pro_activation;
	}


	public boolean isSurprised() {
		return surprised;
	}


	public double getActivation() {
		return activation;
	}

	/**
	 * adds prediction to actual activation;
	 */
	public void integrateActivation() {
		activation += pro_activation;
	}
}
