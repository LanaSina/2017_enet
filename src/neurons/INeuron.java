package neurons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

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

	//weigths
	/** probabilistic input weights*/
	HashMap<INeuron, ProbaWeight> inWeights = new HashMap<INeuron, ProbaWeight>();
	/**direct instantaneous weights*/
	Vector<BundleWeight> directInWeights = new Vector<BundleWeight>();
	/** direct instantaneous weights to pattern neuron. Since these are bundle weights, there can be many weights to the same neurons*/
	Map<INeuron, ArrayList<BundleWeight>> directOutWeights = new HashMap<INeuron,ArrayList<BundleWeight>>();

	/** (id of out neuron, weight) probabilistic outweights (dt=1)*/
	HashMap<INeuron, ProbaWeight> outWeights = new HashMap<INeuron, ProbaWeight>();
	/** (id of out neuron, weight) co-activation weights (dt=0)*/
	HashMap<INeuron, ProbaWeight> coWeights = new HashMap<INeuron, ProbaWeight>();
	//HashMap<INeuron, ProbaWeight> inCoWeights = new HashMap<INeuron, ProbaWeight>();//*/

	
	/** activation of this neuron (real or vitual)*/
	double activation;
	/** predicted activation for next step*/
	double pro_activation;
	/** predicted activation for this step, calcuated at previous step*/
	public double old_pro_activation = 0;
	
	/** has activation been calculated since the last reset or not*/
	private boolean activationCalculated = false;
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
		
		addDirectInWeight(from, false);
		
		for (Iterator<INeuron> iterator = from.iterator(); iterator.hasNext();) {
			INeuron iNeuron = iterator.next();
			if(iNeuron.level>=level){
				level = iNeuron.level++;
			}			
		}
		
		//as outweight from this to To
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, this);
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
		return position;
	}

	public void setCanSnap(boolean canSnap) {
		this.canSnap = canSnap;
	}


	/**
	 * adds new weight, replace it if it existed
	 * @param n
	 * @param p
	 */
	public void addInWeight(INeuron n, ProbaWeight p) {
		if(inWeights.containsKey(n)){
			n.getOutWeights().put(this, p);
		}
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
		ProbaWeight p = null;
			//normal proba weight
			if(inWeights.containsKey(n)){
				//already exists, don't replace
				p = inWeights.get(n);
			}else{
				//create weight
				p = new ProbaWeight(wtype);
				inWeights.put(n, p);
			}
		return p;
	}

	
	/**
	 * add a bundled weight
	 * @param wtype
	 * @param vn vector of in neuron
	 * @return
	 */
	public BundleWeight addDirectInWeight(Vector<INeuron> v, boolean fixed) {	
		//check that this does not already exist
		BundleWeight b = lookForWeight(v);
		
		if(b==null){
			b = new BundleWeight(v, this, fixed);
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
				//Vector<INeuron> in = new Vector<INeuron>(bundle.getInNeurons());
				double[] partial = bundle.getPosition(); //Utils.patternPosition(in);
				p[0] += partial[0];
				p[1] += partial[1];
			}
			p[0] = p[0]/is;
			p[1] = p[1]/is;
			
			//once more for the variance
			for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
				BundleWeight bundle = iterator.next();
				///Vector<INeuron> in = new Vector<INeuron>(bundle.getInNeurons());
				double[] partial = bundle.getPosition();//Utils.patternPosition(in);
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
	
	public void addOrReplaceOutWeight(INeuron n, ProbaWeight p) {
		outWeights.put(n, p);
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
				if(id== 7123 && pair.getKey().getId()==7431){
					Utils.say("increase value  7431->7123");
				}
				if(w.getValue()>Constants.weight_max_age+1){
					throw new java.lang.Error("Value "  + w.getValue() + " is more than max age. Current age: " + w.getAge() +
							". ID " +id+ " from " + pair.getKey().getId()+
							" is in out n " + pair.getKey().getOutWeights().containsKey(this));
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
	public void calculatePredictedActivation() { //TODO change name to calculateProbaActivation
		if(!activationCalculated){
			calculateSurprise();
			old_pro_activation = pro_activation;
			pro_activation = reCalculatePredictedActivation();
			activationCalculated = true;
		}
	}
	
	private void calculateSurprise(){
		setSurprised(false);
		setIllusion(false);
		
		if(pro_activation==0 && activation>0){
			setSurprised(true);
		}	
		if(activation==0 && pro_activation>0){
			setIllusion(true);
		}//*/
		
	}
	
	public double reCalculatePredictedActivation() {
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
		
		/*if(pa>0){
			//trickle down to neurons in patterns?
			Iterator<BundleWeight> it2 = directInWeights.iterator();
			while (it2.hasNext()) {
				BundleWeight b = it2.next();
				b.setPredictedActivation
			}
		}*/
		return pa;
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
		return outWeights;
	}


	public void ageOutWeights() {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = outWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			pair.getValue().increaseAge();
			
			if(id == 7431 && pair.getKey().getId() == 7123)
			Utils.say("age out weight 7431 -> 7123");
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
	

	public HashMap<INeuron, ProbaWeight> getCoWeights() {
		return coWeights;
	}

	/*public HashMap<INeuron, ProbaWeight> getInCoWeights(){
		return inCoWeights;
	}//*/


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
				//age++, value ++ for activated neurons
				//b.increaseActivated();
			}
		}
	}


	public Vector<BundleWeight> getDirectInWeights() {		
		return  directInWeights;
	}
	
	public INeuron getUpperNeuron() {
		Iterator<Entry<INeuron, ArrayList<BundleWeight>>> it = directOutWeights.entrySet().iterator();
		INeuron r = null;
		//while(it.hasNext()){
			Entry<INeuron, ArrayList<BundleWeight>> pair = it.next();
			if(!pair.getKey().isMute()){
				r = pair.getKey();
			} else {
				r = pair.getKey().getUpperNeuron();
			}
		//}
		return r;
	}
	
	/**
	 * tricke down would be more efficient but this is just to show prediction map
	 * @return
	 */
	public double getUpperPrediction() {
		Iterator<Entry<INeuron, ArrayList<BundleWeight>>> it = directOutWeights.entrySet().iterator();
		double p = 0;
		while(it.hasNext()){
			Entry<INeuron, ArrayList<BundleWeight>> pair = it.next();
			if(pair.getKey().getPredictedActivation()>p){
				p = pair.getKey().getPredictedActivation();
				break;
			} else{
				double p2 = pair.getKey().getUpperPrediction();
				if(p2>p){
					p = p2;
				}
			}
		}
		return p;
	}
	
	/**
	 * @param n key: output neuron
	 * @param p value: weight
	 * @return false if the exact same bundle exists or recurrent connection
	 */
	public boolean addDirectOutWeight(INeuron n, BundleWeight p){
		ArrayList<BundleWeight> bs = directOutWeights.get(n);
		if(bs==null){
			bs = new ArrayList<BundleWeight>();
		} else{
			//check that bundle does not exist yet
			for (Iterator<BundleWeight> iterator = bs.iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = iterator.next();
				if(bundleWeight.bundle.size()==1 & bundleWeight.bundle.containsKey(this)){
					return false;
				}
				if(p.sameBundle(bundleWeight.bundle.keySet())){
					return false;
				}
			}
		}
		bs.add(p);
		directOutWeights.put(n, bs);
		return true;
	}

	/**
	 * recursively activate all direct out weights and 
	 * neurons that have all direct in weights activated
	 */
	public void activateDirectOutWeights() {
		Iterator<Entry<INeuron, ArrayList<BundleWeight>>> it = directOutWeights.entrySet().iterator();
		while(it.hasNext()){
			Entry<INeuron, ArrayList<BundleWeight>> pair = it.next();
			INeuron n = pair.getKey();

			for (Iterator<BundleWeight> iterator = pair.getValue().iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = iterator.next();
				bundleWeight.setActivation(1, this);
			}
			
			//do the same for all successive neurons
			//as long as we find ones that were activated by us
			if(!n.isActivated()){//wasn't activated
				n.makeDirectActivation();
				
				if(n.isActivated()){//but now is activated
					n.activateDirectOutWeights();
				}			
			}
		}	
	}


	public void resetDirectOutWeights() {
		for (Iterator<ArrayList<BundleWeight>> iterator = directOutWeights.values().iterator(); iterator.hasNext();) {
			ArrayList<BundleWeight> a = iterator.next();
			for (Iterator<BundleWeight> iterator2 = a.iterator(); iterator2.hasNext();) {
				BundleWeight bundleWeight = iterator2.next();
				bundleWeight.resetActivation(this);
			}
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


	public Map<INeuron, ArrayList<BundleWeight>> getDirectOutWeights() {
		return directOutWeights;		
	}
	
	public void clearDirectOutWeights() {
		directOutWeights.clear();
	}


	/**
	 * @param to_n not used
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
		boolean b = true;
		double[] p2 = bw.getPosition();
		if(p2[0]==0 && p2[1]== 0 && p2[2]== 0 && p2[3]==0){
			throw new Error("No position");
		}
		if(position[0] != p2[0] ||
		   position[1] != p2[1] ||
		   position[2] != p2[2] ||
		   position[3] != p2[3]){
			b = false;
		}
		
		/*for (Iterator<BundleWeight> iterator = directInWeights.iterator(); iterator.hasNext();) {
			BundleWeight ownbw = iterator.next();
			if(ownbw.sameBundle(bw.getInNeurons())){
				b = true;
				break;
			}
		}*/
		return b;
	}
	
	/**
	 * remaps the directs outweights of this neuron so they now 
	 * originate from n. In addition, also modifies the mapping in the output neurons.
	 * @param n
	 */
	public void reportDirectOutWeights(INeuron n){
		//go over the d_out weights 
		for (Iterator<Entry<INeuron, ArrayList<BundleWeight>>> iterator = directOutWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ArrayList<BundleWeight>> pair = iterator.next();
			for (Iterator<BundleWeight> iterator2 = pair.getValue().iterator(); iterator2.hasNext();) {
				BundleWeight bundleWeight = iterator2.next();
				
				//add doutw to n
				n.addDirectOutWeight(pair.getKey(), bundleWeight);
				//not a one-strand recurrent connection
				//if(p){
					//remap output neuron
					INeuron up = pair.getKey();
					up.replaceDirectInWeight(this, n);
				//}
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
			if(pair.getKey() == this){
				//we deal with outweights already
				continue;
			}
			
			ProbaWeight w = pair.getValue();
			pair.getKey().removeOutWeight(this);
			
			//calculate new probability depending on co-activation rate
			double a = w.getProba();
			double b = 0;
			ProbaWeight wn =  n_inWeights.get(pair.getKey());
			if(wn!=null){
				b = wn.getProba();
			} else{
				//make it
				wn = new ProbaWeight(Constants.defaultConnection);
				wn.setAge(w.getAge());
				//value will be set later
				n.addInWeight(pair.getKey(),wn);
				pair.getKey().addOutWeight(n, wn);
			}
			
			double c = 0;
			ProbaWeight wc = coWeights.get(n);
			if(wc!=null){
				c = wc.getProba();
			}
			
			
			double proba = b + (1-c)*a;
			
			wn.setValue((int)(proba*wn.getAge()));//*/
			if(wn.getProba()>1){
				wn.setValue(wn.getAge()+1);
			}
			/*if(wn.getProba()>1 && wn.getAge()==Constants.weight_max_age){
				wn.setValue(wn.getAge());
			}*/
			/*if(b!=proba){
				Utils.say( "initial " + b + " other " + a + " final " + proba + " value " + wn.getProba());
			}*/
		}
	}
	
	/**
	 * remaps the co weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping in the input neurons.
	 * @param n
	 */
	public void reportCoWeights(INeuron n) {
		HashMap<INeuron, ProbaWeight> n_coWeights = n.getCoWeights();
		//remove the weight between the 2
		ProbaWeight p = n_coWeights.remove(this);
		coWeights.remove(n);

		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = coWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight w = pair.getValue();
			//remove weight to this
			pair.getKey().getCoWeights().remove(this);
			
			//calculate new probability depending on co-activation rate
			double a = w.getProba();
			double b = 0;
			ProbaWeight wn =  n_coWeights.get(pair.getKey());
			if(wn!=null){
				b = wn.getProba();
			} else{
				//make it
				wn = new ProbaWeight(Constants.defaultConnection);
				wn.setAge(w.getAge());
				//value will be set later
				n_coWeights.put(pair.getKey(), wn);
				pair.getKey().getCoWeights().put(n, wn);
			}
			
			double c = 0;
			ProbaWeight wc = coWeights.get(n);
			if(wc!=null){
				c = wc.getProba();
			}
			
			//mlog.say("b c a "+ b + " " +c + " " + a);
			double proba = b + (1-c)*a;
			
			if(proba>1){
				proba=1;
			}
			
			//mlog.say("proba " + proba);
			wn.setValue((int)(proba*wn.getAge()));
		}
	}//*/


	/**
	 * remaps the in weights of this neuron so they now 
	 * go to n. In addition, also modifies the mapping in the input neurons.
	 * @param n
	 */
	public void reportInWeights_old(INeuron n) {
		
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = inWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			INeuron from = pair.getKey();
			ProbaWeight w = pair.getValue();
			
			//recurrent weight
			if(from==this){
				//add the inweight to n
				/*n.addInWeight(n,w);
				//remove the inweight from this
				iterator.remove();
				//clean up
				from.removeOutWeight(this);
				//remap
				n.addOrReplaceOutWeight(n, w);*/
			} else{
				//remove the inweight from this
				iterator.remove();
				//clean up
				from.removeOutWeight(this);
			}
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

	/** adds if does not exist 
	 * @return the added or existing weight
	 */
	/*private BundleWeight addDirectOutWeight(BundleWeight b) {
		BundleWeight w;
		if (!directOutWeights.containsKey(to)) {
			directOutWeights.put(to, w);
		} else {
			directOutWeights.get(w);
		}
	}*/

	public void reportDirectOutWeights(INeuron from, INeuron to) {		
		ArrayList<BundleWeight> w = directOutWeights.get(from);
		if(w!=null){// && !directOutWeights.containsKey(to)){ 
			for (Iterator<BundleWeight> iterator = w.iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = iterator.next();
				addDirectOutWeight(to, bundleWeight);
				iterator.remove();
			}
			directOutWeights.remove(from);
		}
	}

	public void clearDirectInWeights() {
		directInWeights.clear();
	}

	/**
	 * check if we have a direct outweight that has only one strand
	 * @return
	 */
	/*public boolean hasSingleDirectOutWeight() {
		boolean r = false;
		for (Iterator<BundleWeight> iterator = directOutWeights.values().iterator(); iterator.hasNext();) {
			BundleWeight b = iterator.next();
			if(b.getBundle().size()==1){
				r = true;
				break;
			}
		}		
		return r;
	}*/

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
			n.getCoWeights().remove(this);
		}
	}//*/

	public void increaseDirectInWeights() {
		Iterator<BundleWeight> it = directInWeights.iterator();
		while(it.hasNext()){
			BundleWeight bw = it.next();
			HashMap<INeuron, ProbaWeight> bundle = bw.getBundle();
			for (Iterator<Entry<INeuron, ProbaWeight>> it2 = bundle.entrySet().iterator(); it2.hasNext();) {
				Entry<INeuron, ProbaWeight> pair = it2.next();
				ProbaWeight w = pair.getValue();
				//value is increased if this weight was previously activated
				if(w.canLearn() && w.isActivated()){
					w.addValue();
					if(w.getValue()>Constants.weight_max_age+1){
						throw new java.lang.Error("Value "  + w.getValue() + " is more than max ag. Current age: " + w.getAge() +
								". ID " +id+ " from " + pair.getKey().getId());
					}
				}
			}
		}
	}


	/** remove all links to this neurons from other neurons*/
	public void removeAllInWeights() {

		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = inWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			pair.getKey().removeOutWeight(this);
		}		
	}

	public void removeDirectInWeight(BundleWeight bundleWeight) {
		directInWeights.remove(bundleWeight);
	}

	public void resetActivationCalculated() {
		activationCalculated = false;		
	}
	
}
