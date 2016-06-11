package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;

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

	HashMap<INeuron, ProbaWeight> inWeights = new HashMap<INeuron, ProbaWeight>();
	/**direct instantaneous weights*/
	HashMap<INeuron, ProbaWeight> directInWeights = new HashMap<INeuron, ProbaWeight>();
	HashMap<INeuron, ProbaWeight> directOutWeights = new HashMap<INeuron, ProbaWeight>();
	//hidden neuron that "covers" this neuron (same that we give direct inweight to)
	//INeuron[] upn = null;

	/** (id of out neuron, weight) probabilistic outweights*/
	HashMap<INeuron, ProbaWeight> outWeights = new HashMap<INeuron, ProbaWeight>();
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
	public ProbaWeight addInWeight(int wtype, INeuron n) {		
		ProbaWeight p;
		if(inWeights.containsKey(n)){
			p = inWeights.get(n);
		}else{
			p = new ProbaWeight(wtype);
			if(wtype==Constants.fixedConnection){
				directInWeights.put(n, p);
			}else{
				inWeights.put(n, p);
			}
		}
		return p;
	}
	
	/*public void setUpperNeuron(INeuron n) {
		upn = new INeuron[1];
		upn[0] = n;
	}*/

	/**
	 * takes the inweight of a neuron and makes this neuron as input.
	 * does not add the connection if similar connection exists
	 * @param p weight
	 * @param id id of the output neuron
	 * @return true if the weight was added, false if it already existed
	 */
	public boolean addOutWeight(ProbaWeight p, INeuron n) {
		boolean b = false;
		if(outWeights.containsValue(p)){
		}else{
			outWeights.put(n, p);
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
		Iterator<Entry<INeuron, ProbaWeight>> it = inWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
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
			
			Iterator<Entry<INeuron, ProbaWeight>> it = inWeights.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<INeuron, ProbaWeight> pair = it.next();
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
		Iterator<Entry<INeuron, ProbaWeight>> it = outWeights.entrySet().iterator();//was inweights orz
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
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
	 * to 1 (must check independently if neuron is activated)
	 */
	private void sendActivations(){
		Iterator<Entry<INeuron, ProbaWeight>> it = outWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.setActivation(1);
		}			
	}


	/**
	 * set all in weights activations to 0
	 */
	public void resetInWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> it = inWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.resetActivation();
		}			
	}


	/**
	 * 
	 * @return output weights of this neuron
	 */
	public HashMap<INeuron, ProbaWeight>  getOutWeights() {
		return (HashMap<INeuron, ProbaWeight>) outWeights.clone();
	}


	public void ageOutWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> it = outWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
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

	
	/**
	 * @return input weights of this neuron
	 */
	public HashMap<INeuron, ProbaWeight> getInWeights() {
		return (HashMap<INeuron, ProbaWeight>) inWeights.clone();
	}


	/**
	 * add a fully formed input weight if it does not already exists
	 * @param pair (key=input neuron id, value = ProbaWeight
	 * @return whether the weight was added or not
	 */
	public boolean addInWeight(Entry<INeuron, ProbaWeight> pair) {
		boolean b = false;
		if(!inWeights.containsKey(pair.getKey())){
			inWeights.put(pair.getKey(), pair.getValue());
			b = true;
		}
		return b;
	}


	/**
	 * checks direct instantaneous inweights and changes activation accordingly
	 */
	public void makeDirectActivation() {
		Iterator<Entry<INeuron, ProbaWeight>> it = directInWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight w = pair.getValue();
			if(w.isActivated()){
				this.increaseActivation(1);
			}
		}				
	}


	public HashMap<INeuron, ProbaWeight> getDirectInWeights() {		
		return (HashMap<INeuron, ProbaWeight>) directInWeights.clone();
	}


	public boolean addDirectInWeight(Entry<INeuron, ProbaWeight> pair) {
		boolean b = false;
		if(!directInWeights.containsKey(pair.getKey())){
			directInWeights.put(pair.getKey(), pair.getValue());
			b = true;
		}
		return b;
	}


	/**
	 * @return the activation of the 1st neuron in the list of directOutWeights
	 */
	public double getUpperPredictedActivation() {
		//should never be null	
		Iterator<Entry<INeuron, ProbaWeight>> it = directOutWeights.entrySet().iterator();
		Map.Entry<INeuron, ProbaWeight> pair = it.next();
		INeuron neuron = pair.getKey();
	
		return neuron.getPredictedActivation();//Predicted
	}


	public boolean addDirectOutWeight(ProbaWeight p, INeuron n2) {
		boolean b = false;
		if(directOutWeights.containsKey(n2)){
			
		}else{
			directOutWeights.put(n2, p);
			b = true;
		}		
		return b;
	}

	/**
	 * 
	 * @param n key: output neuron
	 * @param p value: weight
	 */
	public void setDirectOutWeight(INeuron n, ProbaWeight p){
		directOutWeights.clear();
		directOutWeights.put(n, p);
	}

	public void activateDirectOutWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> it = directOutWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.setActivation(1);
		}	
	}


	public void resetDirectOutWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> it = directOutWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight w = (ProbaWeight) pair.getValue();
			w.resetActivation();
		}		
	}


	/** removes this weight from the list of inweights 
	 * @param key the input neuron */
	public void removeInWeight(INeuron key) {
		inWeights.remove(key);		
	}
	
}
