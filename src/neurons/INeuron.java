package neurons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Set;
import communication.Constants;
import communication.MyLog;
import communication.Utils;

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

	//weigths
	/** probabilistic input weights*/
	HashMap<INeuron, ProbaWeight> inWeights = new HashMap<INeuron, ProbaWeight>();
	/**direct instantaneous weights*/
	Vector<BundleWeight> directInWeights = new Vector<BundleWeight>();
	/** direct instantaneous weights to pattern neuron*/
	HashMap<INeuron, BundleWeight> directOutWeights = new HashMap<INeuron, BundleWeight>();
	/** (id of out neuron, weight) probabilistic outweights (dt=1)*/
	HashMap<INeuron, ProbaWeight> outWeights = new HashMap<INeuron, ProbaWeight>();
	/** (id of out neuron, weight) co-activation weights (dt=0)*/
	HashMap<INeuron, ProbaWeight> coWeights = new HashMap<INeuron, ProbaWeight>();
	HashMap<INeuron, ProbaWeight> inCoWeights = new HashMap<INeuron, ProbaWeight>();

	
	/** activation of this neuron (real or vitual)*/
	double activation;
	/** predicted activation (positive) */
	double pro_activation;
	
	/** has activation been calculated since the last reset or not*/
	public boolean activationCalculated = false;
	/** predicted activation */
	int predictedActivation;
	/** has activation been calculated since the last reset or not*/
	boolean predictedActCalculated = false;
	/** does the prediction corresponds to any kind of input*/
	private boolean surprised = false;
	/** for performance caclualtion*/
	private boolean illusion = false;
	/** used when pruning neurons*/
	public boolean justSnapped = true;
	
	/** whether we can get remembered in the STM or not (eq to consciousness)*/
	boolean mute = false;
	/** whether it can snap with other neurons or not (sensory neurons cannot)*/
	boolean canSnap = true;
	
	public int level = 0;
	
	//this is used to check if pattern neurons are identical or not
	/**on direct inweights: mean(x), mean (y), var(x), var(y))*/
	private double[] position = {0,0,0,0};

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
		
		addDirectInWeight(from);
		
		for (Iterator<INeuron> iterator = from.iterator(); iterator.hasNext();) {
			INeuron iNeuron = iterator.next();
			if(iNeuron.level>=level){
				level = iNeuron.level++;
			}			
		}
		
		//as outweight from this to To
		ProbaWeight p = to.addInWeight(Constants.fixedConnection, this);
		outWeights.put(to, p);
		
		//position
		setPosition(Utils.patternPosition(from));
	}
	

	public boolean canSnap() {
		return canSnap;
	}
	
	/**
	 * @param p on direct inweights: mean(x), mean (y), var(x), var(y) (also check number of direct inw))
	 */
	public void setPosition(double[] p) {
		position = p.clone();
	}
	
	public double[] getPosition() {
		return position.clone();
	}

	public void setCanSnap(boolean canSnap) {
		this.canSnap = canSnap;
	}


	/**
	 * adds new weight if it did not exist
	 * @param n
	 * @param p
	 */
	public void addInWeight(INeuron n, ProbaWeight p) {
		if(!inWeights.containsKey(n)){
			inWeights.put(n, p);
		}
	}

	/**
	 * Adds a neuron as input to this.
	 * 
	 * @param wtype type of the input connection
	 * @param id id of the input neuron
	 * @return existing or newly created connection.
	 */
	public ProbaWeight addInWeight(int wtype, INeuron n) {		
		ProbaWeight p = null;
		if(wtype == Constants.defaultConnection){
			//normal proba weight
			if(inWeights.containsKey(n)){
				//already exists, don't replace
				p = inWeights.get(n);
			}else{
				//create weight
				p = new ProbaWeight(wtype);
				inWeights.put(n, p);
			}
		} 
		return p;
	}

	
	/**
	 * add a bundled weight
	 * @param wtype
	 * @param vn vector of in neuron
	 * @return
	 */
	public BundleWeight addDirectInWeight(Vector<INeuron> v) {	
		//check that this does not already exist
		BundleWeight b = lookForWeight(v);
		
		if(b==null){
			b = new BundleWeight(v, this);
		}
		
		directInWeights.addElement(b);
		recalculatePosition();
		
		return b;
	}
	
	
	public void recalculatePosition() {
		//recalculate postion
		double[] p = {0,0,0,0};
		int is = directInWeights.size();
		if(is==1){
			Vector<INeuron> in = new Vector<INeuron>(directInWeights.get(0).getInNeurons());
			p = Utils.patternPosition(in);
		}else{
			for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
				BundleWeight bundle = iterator.next();
				Vector<INeuron> in = new Vector<INeuron>(bundle.getInNeurons());
				double[] partial = Utils.patternPosition(in);
				p[0] += partial[0];
				p[1] += partial[1];
			}
			p[0] = p[0]/is;
			p[1] = p[1]/is;
			
			//once more for the variance
			for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
				BundleWeight bundle = iterator.next();
				Vector<INeuron> in = new Vector<INeuron>(bundle.getInNeurons());
				double[] partial = Utils.patternPosition(in);
				p[2] += Math.pow(p[0]-partial[0], 2);
				p[3] += Math.pow(p[1]-partial[1], 2);
			}
			p[2] = p[2]/is;
			p[3] = p[3]/is;
		}
		
		setPosition(p);
	}
	
	/**
	 * 
	 * @param v
	 * @return null if weights does not exist, or existing weight
	 */
	private BundleWeight lookForWeight(Vector<INeuron> v) {
		BundleWeight p = null;
		for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			HashSet<INeuron> s = new HashSet<INeuron>(v);
			if(b.sameBundle(s)){
				p = b;
				break;
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
		if(outWeights.containsKey(n)){
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
				if(w.getValue()>Constants.weight_max_age+1){
					throw new java.lang.Error("Value "  + w.getValue() + " is more than max ag. Current age: " + w.getAge() +
							". ID " +id+ " from " + pair.getKey().getId());
				}
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
	 * if there is not predicted activation but we are activated, the neuron should be "surprised"
	 * we don't count this as surprised if an upper pattern is activated 
	 * 
	 * */
	//TODO surprise: no input is not same as 0 input
	public void calculateActivation() { //TODO change name to calculateProbaActivation
		if(!activationCalculated){
			setSurprised(false);
			setIllusion(false);
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
			

			if(pro_activation==0 && activation>0){
				setSurprised(true);
			}	
			if(activation==0 && pro_activation>0){
				setIllusion(true);
			}
			
	
			pro_activation = pa;
			activationCalculated = true;
		}
	}
	
	/** activated diect outweights if this neuron is activated */
	public void propagateActivation(){
		if(isActivated()){
			//activate direct out weights
			activateDirectOutWeights();			
		}
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
	 */
	public double getPredictedActivation() {		
		return pro_activation;
	}


	public boolean isSurprised() {
		return surprised;
	}
	
	public boolean isIllusion() {
		return illusion;
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
		return inWeights;// (HashMap<INeuron, ProbaWeight>) 
	}
	
	/**
	 * 
	 * @return
	 */
	public HashMap<INeuron, ProbaWeight> getCoWeights() {
		return coWeights;
	}
	
	/**
	 * 
	 * @return
	 */
	public HashMap<INeuron, ProbaWeight> getInCoWeights(){
		return inCoWeights;
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
		Iterator<BundleWeight> it = directInWeights.iterator();
				
		while(it.hasNext()){
			BundleWeight b = it.next();
			if(b.bundleIsActivated()){
				this.increaseActivation(1);
				//mute bundle
				b.muteInputNeurons();
				//unmute self just in case we had a recurrent connection
				this.setMute(false);
			}
		}
	}


	public Vector<BundleWeight> getDirectInWeights() {		
		return (Vector<BundleWeight>) directInWeights.clone();
	}


	/**
	 * used to build the prediction map (called by eye sensors on eye neurons)
	 * the "upper neuron" is the neuron which has the same receptive field, or a snapped neuron
	 * @return the activation of the 1st neuron in the list of directOutWeights
	 */
	public double getUpperPredictedActivation() {
		if(directOutWeights.size()>1){
			throw new Error("=========ERROR");
		}
		//should never be null	
		Iterator<Entry<INeuron, BundleWeight>> it = directOutWeights.entrySet().iterator();
		Entry<INeuron, BundleWeight> pair = it.next();
		INeuron neuron = pair.getKey();
		return neuron.getPredictedActivation();
	}
	
	/** for prediction map debug and perf calculation */
	public boolean getUpperSurprised() {
		/*if(!isActivated()){
			return false;
		}
		
		Iterator<INeuron> it = directOutWeights.keySet().iterator();
		while(it.hasNext()){
			INeuron up = it.next();
			if(up.isActivated() && !up.isSurprised()){
				return false;
			}
		}
		
		//default
		return true;*/
		Iterator<INeuron> it = directOutWeights.keySet().iterator();
		INeuron up = it.next();
		return up.isSurprised();
	}
	
	public boolean getUpperIllusion(){
		/*if(isActivated()){
			return false;
		}
		
		Iterator<INeuron> it = directOutWeights.keySet().iterator();
		while(it.hasNext()){
			INeuron up = it.next();
			if(up.isActivated() && !up.isIllusion()){
				return false;
			}
		}
		
		//default
		return true;*/
		
		Iterator<INeuron> it = directOutWeights.keySet().iterator();
		INeuron up = it.next();
		return up.isIllusion();
	}
	
	/**
	 * @param n key: output neuron
	 * @param p value: weight
	 * @return existing or added probaweight; null if recurrent connection
	 */
	public BundleWeight addDirectOutWeight(INeuron n, BundleWeight p){
		BundleWeight r;
		if(n == this){
			return null;
		}
		if(!directOutWeights.containsKey(n)){
			directOutWeights.put(n, p);
			r = p;
		}else{
			r = directOutWeights.get(n);
		}		
		return r;
	}

	/**
	 * recursively activate all direct out weight and 
	 * neurons that have all direct in weights activated
	 */
	public void activateDirectOutWeights() {
		Iterator<Entry<INeuron, BundleWeight>> it = directOutWeights.entrySet().iterator();
		while(it.hasNext()){
			Entry<INeuron, BundleWeight> pair = it.next();
			ProbaWeight pw = pair.getValue();
			INeuron n = pair.getKey();
			pw.setActivation(1, n);
			
			/*if(pw.canLearn()){
				pw.increaseAge();
			}*/
			
			
			//do the same for all successive neurons
			//as long as we find ones that were activated by us
			
			if(!n.isActivated()){//wasn't activated
				//mlog.say("not activated yet "+ n.id);
				n.makeDirectActivation();
				
				if(n.isActivated()){//but now is activated
					n.activateDirectOutWeights();
				}			
			}
		}	
	}


	public void resetDirectOutWeights() {
		for (Iterator<BundleWeight> iterator = directOutWeights.values().iterator(); iterator.hasNext();) {
			BundleWeight p = iterator.next();
			p.resetActivation();
		}	
	}


	/** removes this weight from the list of inweights 
	 * @param key the input neuron */
	public void removeInWeight(INeuron key) {
		inWeights.remove(key);	
	}


	public boolean removeOutWeight(INeuron key) {
		if(outWeights.remove(key)==null){
			return false;
		}
		return true;
	}


	public HashMap<INeuron, BundleWeight> getDirectOutWeights() {
		return (HashMap<INeuron, BundleWeight>) directOutWeights.clone();		
	}
	
	public void clearDirectOutWeights() {
		directOutWeights.clear();
	}


	/**
	 * @return true if one of the inweights is a bundleweights with these in-neurons
	 */
	public boolean sameBundleWeights(Vector<INeuron> neurons, INeuron to_n) {
		boolean b = false;
		//iterate over bundleweights
		for(Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
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

	
	/**
	 * @param bw
	 * @return true if we possess a similar bundleweight
	 */
	private boolean sameDirectInWeight(BundleWeight bw) {
		boolean b = false;
		for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight ownbw = iterator.next();
			if(ownbw.sameBundle(bw.getInNeurons())){
				b = true;
				break;
			}
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
		for (Iterator<Entry<INeuron, BundleWeight>> iterator = directOutWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, BundleWeight> pair = iterator.next();
			//add doutw to n
			BundleWeight p = n.addDirectOutWeight(pair.getKey(), pair.getValue());
			//not a one-strand recurrent connection
			if(p != null){
				//remap output neuron
				INeuron up = pair.getKey();
				up.replaceDirectInWeight(this, n);
			}
		}
		
		directOutWeights.clear();
	}

	/**
	 * replaces all instances of "replaced" in bundles by "replacement" (avoiding strictly recurrent connections)
	 * @param replaced
	 * @param replacement
	 */
	private void replaceDirectInWeight(INeuron replaced, INeuron replacement) {
		for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			b.replace(replaced, replacement);	
		}
		recalculatePosition();
	}

	/**
	 * remaps the in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping in the input neurons.
	 * @param n
	 */
	public void reportInWeights(INeuron n) {
		HashMap<INeuron, ProbaWeight> n_inWeights = n.getInWeights();
		
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = inWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight w = pair.getValue();
			
			//calculate new probability depending on co-activation rate
			double a = w.getProba();
			double b = 0;
			ProbaWeight wn =  n_inWeights.get(pair.getKey());
			if(wn!=null){
				b = wn.getProba();
			}
			double c = 0;
			ProbaWeight wc = coWeights.get(n);
			if(wc!=null){
				c = wc.getProba();
			}
			
			double proba = b + (1-c)*a;
			wn.setValue((int)proba*Constants.weight_max_age);
		}
	}


	/**
	 * remaps the direct in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping of the output neurons.
	 * if they exist, only common inweights are kept (generalisation)
	 * @param n
	 */
	public void reportDirectInWeights(INeuron n) {	
		for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			
			if(n.addDirectInWeight(b)){
				b.notifyChange(this,n);
			} else{
				//just delete it
				b.notifyRemoval(this);
			}
			iterator.remove();
		}
		
		directInWeights.clear();
	}	
	
	public boolean addDirectInWeight(BundleWeight bw) {		
		if(sameDirectInWeight(bw)){
			return false;
		}
		directInWeights.add(bw);
		return true;
	}

	/**
	 * remove the weight mapped to n
	 * @param n
	 */
	void removeDirectOutWeight(INeuron n) {
		directOutWeights.remove(n);
	}

	/**
	 * delete the links from this neuron in other neurons
	 */
	public void removeAllOutWeights() {
		
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = outWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			pair.getKey().removeInWeight(this);
		}
	}

	/**
	 * 
	 * @param n2
	 * @return true is n2 is part of any pattern here
	 */
	public boolean directInWeightsContains(INeuron n2) {
		boolean b = false;
		for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight bw = iterator.next();
			if(bw.contains(n2)){// & bw.getBundle().size()==1){
				b = true;
				break;
			}
		}
		
		return b;
	}


	public void reportDirectOutWeights(INeuron from, INeuron to) {		
		BundleWeight w = directOutWeights.get(from);
		if(w!=null && !directOutWeights.containsKey(to)){ //must make it possible to have many direct outweights
			directOutWeights.put(to, w);
		}
		directOutWeights.remove(from);
	}

	public void clearDirectInWeights() {
		directInWeights.clear();
	}

	/**
	 * check if we have a direct outweight that has only one strand
	 * @return
	 */
	public boolean hasSingleDirectOutWeight() {
		boolean r = false;
		for (Iterator<BundleWeight> iterator = directOutWeights.values().iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			if(b.getBundle().size()==1){
				r = true;
				break;
			}
		}		
		return r;
	}

	public void setSurprised(boolean surprised) {
		this.surprised = surprised;
	}
	
	public void setIllusion(boolean i) {
		this.illusion = i;
	}

	public int countInWeights() {
		return inWeights.size();
	}

	/**
	 * update mappings in neuron linked to this one
	 */
	public void removeCoWeights() {
		Iterator<Entry<INeuron, ProbaWeight>> co_it = coWeights.entrySet().iterator();
		while (co_it.hasNext()) {
			Entry<INeuron, ProbaWeight> pair = co_it.next();
			INeuron n = pair.getKey();
			n.getInCoWeights().remove(this);
		}
		
		Iterator<Entry<INeuron, ProbaWeight>> i_co_it = inCoWeights.entrySet().iterator();
		while (i_co_it.hasNext()) {
			Entry<INeuron, ProbaWeight> pair = i_co_it.next();
			INeuron n = pair.getKey();
			n.getCoWeights().remove(this);
		}
		
	}

	public void increaseDirectInWeights() {
		Iterator<BundleWeight> it = directInWeights.iterator();
		while(it.hasNext()){
			BundleWeight bw = it.next();
			HashMap<INeuron, ProbaWeight> bundle = bw.getBundle();
			for (Iterator<Entry<INeuron, ProbaWeight>> it2 = bundle.entrySet().iterator(); it2.hasNext();) {
				Entry<INeuron, ProbaWeight> pair = it2.next();
				ProbaWeight w = pair.getValue();
				//value is increased if this weight was previously activated
				if(w.canLearn() & w.isActivated()){
					w.addValue();
					if(w.getValue()>Constants.weight_max_age+1){
						throw new java.lang.Error("Value "  + w.getValue() + " is more than max ag. Current age: " + w.getAge() +
								". ID " +id+ " from " + pair.getKey().getId());
					}
				}
			}
		}
	}
	
}
