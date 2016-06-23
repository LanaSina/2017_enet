package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;
import com.sun.org.apache.xpath.internal.operations.And;

import apple.laf.JRSUIUtils.Tree;
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

	/** probabilistic input weights*/
	HashMap<INeuron, ProbaWeight> inWeights = new HashMap<INeuron, ProbaWeight>();
	/**direct instantaneous weights*/
	HashMap<INeuron, ProbaWeight> directInWeights = new HashMap<INeuron, ProbaWeight>();
	HashMap<INeuron, ProbaWeight> directOutWeights = new HashMap<INeuron, ProbaWeight>();

	/** (id of out neuron, weight) probabilistic outweights*/
	HashMap<INeuron, ProbaWeight> outWeights = new HashMap<INeuron, ProbaWeight>();
	/** activation of this neuron (real or vitual)*/
	double activation;
	/** predicted activation (positive) */
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
	/**pattern weights that input to this neuron, and output to someone else*/
	Vector<BundleWeight> bundleWeights = new Vector<BundleWeight>();

	
	/** whether we can get remembered in the STM or not (eq to consciousness)*/
	boolean mute = false;
	
	public INeuron(int id) {
		super(id);
	}
	
	/**
	 * moved from PNeuron class
	 * creates a pattern neuron: neuron that can be activated if a specific pattern of neurons were 
	 * activated at t-1
	 * @param stm list of pre-neurons
	 * @param n neuron to make the bundleWeigth to
	 */
	public INeuron(Vector<INeuron> from, INeuron to, int id) {//TODO make "to" as a vector
		super(id);
		mlog.setName("Pattern Neuron");

		BundleWeight b = new BundleWeight(from, to);
		//as special inweight to this
		bundleWeights.add(b);
		//as outweight from this to To
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, this);
		outWeights.put(to, p);
	}

	/**
	 * adds or replace new weight (there isnt supposed to be contradictions in inweights
	 * @param n
	 * @param p
	 */
	private void addInWeight(INeuron n, ProbaWeight p) {
		inWeights.put(n, p);
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

	/**
	 * takes the inweight of a neuron and makes this neuron as input.
	 * does not add the connection if similar connection exists
	 * @param n output neuron
	 * @param p weight
	 * @return true if the weight was added, false if it already existed
	 */
	public boolean addOutWeight(INeuron n, ProbaWeight p) {
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
		mute = false;
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
			ProbaWeight w = pair.getValue();
			//value is increased if this weight was previously activated
			if(w.canLearn() & w.isActivated()){
				w.addValue();
			}
		}
	}

	/**
	 * recalculates activation
	 * @return true is activation is positive, false otherwise
	 */
	public boolean isActivated() {	
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
			//calculate predicted positive activation
			double pa = 0;

			double confidence = Constants.confidence_threshold;
			
			Iterator<Entry<INeuron, ProbaWeight>> it = inWeights.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<INeuron, ProbaWeight> pair = it.next();
				ProbaWeight pw = pair.getValue();
				double w  = pw.getProba();
				if(w>confidence & pw.isActivated()){
					pa+=1;
				}
			}
	
	
			if(pro_activation==0 & activation>0){
				surprised = true;
			}
			
			
			pro_activation = pa;
			activationCalculated = true;
			
			if(isActivated()){
				//activate direct out weights
				activateDirectOutWeights();
				
				//moved from PNeuron
				//mute down neurons
				it = directInWeights.entrySet().iterator();
				while(it.hasNext()){
					Map.Entry<INeuron, ProbaWeight> pair = it.next();
					ProbaWeight w = pair.getValue();
					//mute sensory neurons
					w.muteInputNeurons();
				}
				//unmute self just in case we had a recurrent connection
				this.setMute(false);
			}
			
			
		}
	}
	
	public void calculateActivation(boolean b) {
		if(true){
			activationCalculated = false;
		}
		calculateActivation();
	}


	/**
	 * reset output weights activation to 0.
	 */
	public void resetOutWeights() {
		Iterator<ProbaWeight> it = outWeights.values().iterator();
		while(it.hasNext()){
			ProbaWeight w = it.next();
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
			ProbaWeight pw = pair.getValue();
			pw.setActivation(1,this);
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

		for (Iterator<ProbaWeight> iterator = outWeights.values().iterator(); iterator.hasNext();) {
			ProbaWeight p = iterator.next();
			p.increaseAge();
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
		//TODO bundleweight could have cleaner implementation... maybe put it in inweights
		// pattern contained inside other patterns should be muted
		Vector<BundleWeight> activated = new Vector<BundleWeight>();
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			if(b.bundleIsActivated()){
				this.increaseActivation(1);
				activated.addElement(b);
				//mute bundle
				b.muteInputNeurons();
			}		
		}
		//mute secondary patterns
		for (Iterator<BundleWeight> iterator = activated.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();		
			for (Iterator<BundleWeight> iterator2 =  bundleWeights.iterator(); iterator2.hasNext();) {
				BundleWeight b2 = iterator2.next();
				if(!b.equals(b2) && b.getInNeurons().containsAll(b2.getInNeurons())){
					//b2.setActivation(0);//muting only is not enough
					//mlog.say("deactivated secondary pattern");
				}
			}
			
		}

	}


	public HashMap<INeuron, ProbaWeight> getDirectInWeights() {		
		return (HashMap<INeuron, ProbaWeight>) directInWeights.clone();
	}


	/**
	 * 
	 * @param pair
	 * @return true if the weight was added, false if it already existed
	 */
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


	/**
	 * 
	 * @param p
	 * @param n2
	 * @return true if the weight was added, false if it already existed
	 */
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
	 * TODO do not use this directly (because "clear" is dangerous)
	 * @param n key: output neuron
	 * @param p value: weight
	 */
	public void setDirectOutWeight(INeuron n, ProbaWeight p){
		directOutWeights.clear();
		directOutWeights.put(n, p);
	}
	
	/**
	 * @param n key: output neuron
	 * @param p value: weight
	 * @return existing or added probaweight
	 */
	public ProbaWeight addDirectOutWeight(INeuron n, ProbaWeight p){
		ProbaWeight r;
		if(!directOutWeights.containsKey(n)){
			directOutWeights.put(n, p);
			r = p;
		}else{
			r = directOutWeights.get(n);
		}
		
		return r;
	}

	public void activateDirectOutWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> it = directOutWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight pw = (ProbaWeight) pair.getValue();
			pw.setActivation(1,this);
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
		//remove from bundle weights too
		/*for (Iterator<BundleWeight> it = bundleWeights.iterator(); it.hasNext();) {
			BundleWeight b = it.next();
			b.removeStrand(key);
		}
		bundleWeights.remove(key);*/
	}


	public boolean removeOutWeight(INeuron key) {
		if(outWeights.remove(key)==null){
			return false;
		}
		return true;
	}


	public HashMap<INeuron, ProbaWeight> getDirectOutWeights() {
		return (HashMap<INeuron, ProbaWeight>) directOutWeights.clone();		
	}


	/**
	 * @return true if one of the inweights is a bundleweights with these in-neurons
	 */
	public boolean sameBundleWeights(Vector<INeuron> neurons, INeuron to_n) {
		boolean b = false;
		//iterate over bundleweights
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight bw = iterator.next();
			Set<INeuron> v = bw.getInNeurons();
			if(v.containsAll(neurons) && neurons.containsAll(v)){//exactly equal
				if(outWeights.containsKey(to_n)){
					b = true;
					break;
				}
			}
		}
		return b;
	}


	public void setMute(boolean b) {
		mute = b;
	}


	public boolean isMute() {
		return mute;
	}


	public Vector<BundleWeight> getBundleWeights() {
		return (Vector<BundleWeight>) bundleWeights.clone();	
	}

	
	private boolean addBundleWeight(BundleWeight bw) {
		//moved from PNeuron
		boolean b = false;
		if(!sameBundleWeight(bw)){	
			bundleWeights.add(bw);
			b = true;
		}
		return b;
	}
	
	/**
	 * @param bw
	 * @return true if we possess a similar bundleweight
	 */
	private boolean sameBundleWeight(BundleWeight bw) {
		boolean b = false;
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight ownbw = iterator.next();
			if(ownbw.sameBundle(bw.getInNeurons())){
				b = true;
				break;
			}
		}
		return b;
	}

	/**
	 * 
	 * @param n
	 * @return true if n was present as direct in weight
	 */
	public boolean removeDirectInWeight(INeuron n) {
		boolean b;
		if(directInWeights.remove(n) != null){
			b = true;
		} else{
			b = false;
		}
		return b;
	}
	
	/**
	 * remaps the directs outweights of this neuron so they now 
	 * originate from n. In addition, also modifies the mapping in the output neurons.
	 * @param n
	 */
	public void reportDirectOutWeights(INeuron n){
		//go over the dout weights
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = directOutWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			//add doutw to n
			if(n.addDirectOutWeight(pair.getValue(), pair.getKey())){
				//remap output neuron
				INeuron up = pair.getKey();
				ProbaWeight p = pair.getValue();
				if(up.removeDirectInWeight(this)){
					up.addDirectInWeight(n,p);
				}
			}
		}
		
		//remove self from bundle weights
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			b.replace(this, n);
		}

	}

	private void replaceInBundle(INeuron replaced, INeuron replacement) {
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			b.replace(replaced, replacement);	
		}
	}

	private void addDirectInWeight(INeuron n, ProbaWeight p) {
		directInWeights.put(n, p);	
	}

	/**
	 * remaps the in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping in the input neurons.
	 * @param n
	 */
	public void reportInWeights(INeuron n) {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = inWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			INeuron from = pair.getKey();
			ProbaWeight w = pair.getValue();
			//recurrent weight
			if(from==this){
				from = n;
			}
			//add the inweight to n
			n.addInWeight(from,w);
			//remap (are supposed to be same anyway)
			from.addOutWeight(n, w);			
		}
		//remove inweights from this neuron
		inWeights.clear();
	}

	/**
	 * remaps the direct in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping of the output neurons.
	 * @param n
	 */
	public void reportDirectInWeights(INeuron n) {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = directInWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			if(n.addDirectInWeight(pair)){
				INeuron in = pair.getKey();
				ProbaWeight p = pair.getValue();
				in.addDirectOutWeight(n, p);
				in.removeDirectOutWeight(this);
			}
		}		
	}

	/**
	 * remaps the bundle in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping of the output neurons.
	 * @param n
	 */
	public void reportBundleWeights(INeuron n) {
		for (Iterator<BundleWeight> iterator = bundleWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			//remove self from bundle weight
			ProbaWeight p = b.removeStrand(this);
			if(p!=null){
				//add n instead
				b.addStrand(n,p);
				n.addOutWeight(n, b);
			}
			
			if(n.addBundleWeight(b)){
				//re-route down neurons to n
				Set<INeuron> ins = b.getInNeurons();
				for (Iterator<INeuron> it2 = ins.iterator(); iterator.hasNext();) {
					INeuron in = it2.next();
					in.addDirectOutWeight(n, b.getStrand(in));
					in.removeDirectOutWeight(this);
				}				
			}
			
		}
	}
	
	
	/**
	 * remove the weight mapped to n
	 * @param n
	 */
	private void removeDirectOutWeight(INeuron n) {
		directOutWeights.remove(n);
	}

	/**
	 * delete the links from this neuron in other neurons
	 */
	public void removeAllOutWeights() {
		for (Iterator<INeuron> iterator = outWeights.keySet().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			n.removeInWeight(this);
		}
	}

	public boolean directInWeightsContains(INeuron n2) {
		boolean b = false;
		for (Iterator<INeuron> iterator = directInWeights.keySet().iterator(); iterator.hasNext();) {
			INeuron from = iterator.next();
			if(from==n2){
				b = true;
				break;
			}
		}
		
		return b;
	}
	
}
