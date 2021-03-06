package neurons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.Set;
import java.util.Vector;

import communication.Constants;
import communication.Utils;

/**
 * This is a weight made of a bundle of FixedWeights (weight with fixed values).
 * It is only activated if all inweights without exception are activated
 * 
 * @author lana
 *
 */
public class BundleWeight extends ProbaWeight {
	
	/** the inweights*/
	HashMap<INeuron, ProbaWeight> bundle = new HashMap<>();//<input,weight>
	/**on inweights: mean(x), mean (y), var(x), var(y))*/
	private double[] position = {0,0,0,0};
	
	/**
	 * This constructor might never be used
	 * @param type 
	 */
	public BundleWeight(int type) {
		super(type);
		//mlog.setName("BWeight");
	}
	
	/**
	 * 
	 * @param from
	 * @param to not actually used
	 */
	public BundleWeight(Vector<INeuron> from, INeuron to, boolean fixed) {		
		//create age and value
		super(Constants.defaultConnection);
		
		//create bundle
		for (Iterator<INeuron> iterator = from.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			ProbaWeight p;
			if(!fixed){
				p = new ProbaWeight(Constants.defaultConnection);
				p.setValue(1);
			}else {
				p = new ProbaWeight(Constants.fixedConnection);
			}
			bundle.put(n, p);
			n.addDirectOutWeight(to, this);
		}
		
		recalculatePosition();
	}
	
	/**
	 * @param p on direct inweights: mean(x), mean (y), var(x), var(y))
	 */
	private void setPosition(double[] p) {
		position = p;
	}
	
	public void recalculatePosition() {
		//recalculate postion
		double[] p = {0,0,0,0};
		
			
		Vector<INeuron> in = new Vector<INeuron>(getInNeurons());
		double is = in.size();
		double[] partial = Utils.patternPosition(in);
		p[0] += partial[0];
		p[1] += partial[1];
		
		p[0] = p[0]/is;
		p[1] = p[1]/is;
		
		for (Iterator iterator = in.iterator(); iterator.hasNext();) {
			INeuron iNeuron = (INeuron) iterator.next();
			p[2] += Math.pow(p[0]-partial[0], 2);
			p[3] += Math.pow(p[1]-partial[1], 2);
		}
		
		p[2] = p[2]/is;
		p[3] = p[3]/is;
		
		setPosition(p);
	}
	
	public double[] getPosition() {
		return position;
	}

	
	/**
	 * @return true if all weights in bundle are activated, false otherwise
	 */
	public boolean bundleIsActivated() {
		
		boolean b = true;
		Vector<INeuron> remove = new Vector<INeuron>();
		//>90% of 0.9 weights must be activated
		//>70% of 0.7 weights etc
		//int activated = 0;
		double sum = 0; 
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight pw = pair.getValue();
			if(!pw.isActivated()){
				//activated++;
				if(pw.getProba()>(1.0/20)){
					sum+=pw.getProba();
				}else {
					remove.addElement(pair.getKey());
				}
				if(sum>=1){
					return false;
				}
			}
		}
		
		for (Iterator iterator = remove.iterator(); iterator.hasNext();) {
			INeuron n = (INeuron) iterator.next();
			removeStrand(n);
			n.removeDirectInWeight(this);
		}
		
		if(remove.size()>0){
			
		}
		
		return b;
	}
	
	
	/**
	 * @return true if bundle is the same
	 */
	@Override
	public boolean sameBundle(Set<INeuron> neurons) {
		boolean b = true;
		if(!neurons.equals(bundle.keySet())){
			b = false;
		}
		return b;
	}
	
	@Override
	public void muteInputNeurons() {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			INeuron n = pair.getKey();
			n.setMute(true);
		}
	}

	public Set<INeuron> getInNeurons() {	
		return bundle.keySet();
	}

	public ProbaWeight getStrand(INeuron n2) {	
		return bundle.get(n2);
	}

	public ProbaWeight removeStrand(INeuron key) {
		if(bundle.isEmpty()){
			//mlog.say("********** empty");
		}
		return bundle.remove(key);
	}
	
	/**
	 * notify input neurons of the disparition of the output neuron
	 */
	public void notifyRemoval(INeuron removed) {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			pair.getKey().removeDirectOutWeight(removed);
			iterator.remove();
		}
	}

	/**
	 * replace the neuron
	 * @param replaced
	 * @param replacement
	 */
	public void replace(INeuron replaced, INeuron replacement) {
		if(bundle.containsKey(replaced)){
			ProbaWeight p = bundle.remove(replaced);
			//avoid recurrent connections if bundle has only one strand
			if(replaced!=replacement || bundle.size()>0){
				//avoid recurrent connections if bundle has only one strand
				bundle.put(replacement, p);
				
			}
		}		
	}

	
	@Override
	public HashMap<INeuron, ProbaWeight> getBundle() {
		return bundle;
	}
	
	
	/**
	 * each strand will have its own value. the bundle itself is 1.
	 */
	@Override
	public int getValue() {
		return 1;
	}

	public void addStrand(INeuron n, ProbaWeight p) {
		bundle.put(n, p);
	}

	//TODO maybe bundeweights and probaweights just descend from the same interface
	//bc bw doesnt need the "activation" var for example
	@Override
	public void setActivation(int a, INeuron n) {
		bundle.get(n).setActivation(1, n);
	}
	
	//@Override
	/**
	 * reset the strand coming from n
	 */
	public void resetActivation(INeuron n) {
		bundle.get(n).resetActivation();
		/*for (Iterator<ProbaWeight> iterator = bundle.values().iterator(); iterator.hasNext();) {
			ProbaWeight p = iterator.next();
			p.resetActivation();
		}*/		
	}

	public boolean contains(INeuron n2) {
		return bundle.containsKey(n2);
	}

	/**tell to in neurons to change their outweights map
	 * 
	 * @param from
	 * @param to
	 */
	public void notifyChange(INeuron from, INeuron to) {
		for (Iterator<INeuron> iterator3 = bundle.keySet().iterator(); iterator3.hasNext();) {
			INeuron n = iterator3.next();
			n.reportDirectOutWeights(from,to);
		}
	}

	/**
	 * decrease probabilities all neurons that are not in newBundle and calculates activation.
	 * @param newBundle
	 */
	public void decreaseAllBut(Vector<INeuron> newBundle) {
		
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight p = pair.getValue();
			if(p.canLearn()){
				p.increaseAge();
				if(newBundle.contains(pair.getKey())){
					p.addValue();
				}
				//mlog.say(" "+p.getProba());
			}
		}
		//mlog.say("------- ");
	}

	public void increaseActivated() {
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight p = pair.getValue();
			if(p.canLearn()){
				p.increaseAge();
				p.addValue();
			}
		}
		
	}

	/**
	 * also remove the mapping of the from neuron
	 * removes useless weights (value = 1)
	 * @param to
	 */
	public void removeYoungWeigths(INeuron to, INeuron from, Iterator<Entry<INeuron, ArrayList<BundleWeight>>>  it) {
		boolean changed = false;
		//Vector<ProbaWeight> v = new Vector<ProbaWeight>();
		
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight p = pair.getValue();
			if(p.getValue()==1 && !p.canLearn()){
				//v.addElement(p);
				iterator.remove();
				if(pair.getKey()==from){
					it.remove();
				} else {
					pair.getKey().removeDirectOutWeight(to);
					changed = true;
				}
			}
		}
		
		if(changed){
			recalculatePosition();
			to.recalculatePosition();
		}
		
		//return v;
		
	}

	/**
	 * 
	 * @param sTM
	 * @return true is this combination can activate this bundle
	 */
	public boolean bundleIsActivated(Vector<INeuron> sTM) {
		boolean b = true;
		Vector<INeuron> remove = new Vector<INeuron>();
		//>90% of 0.9 weights must be activated
		//>70% of 0.7 weights etc
		//int activated = 0;
		double sum = 0; 
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			INeuron n = pair.getKey();
			if(!sTM.contains(n)){
				ProbaWeight pw = pair.getValue();
				if(pw.getProba()>(1.0/20)){
					sum+=pw.getProba();
				}else {
					remove.addElement(pair.getKey());
				}
				if(sum>=1){
					return false;
				}
			}
		}
		
		for (Iterator iterator = remove.iterator(); iterator.hasNext();) {
			INeuron n = (INeuron) iterator.next();
			removeStrand(n);
			n.removeDirectInWeight(this);
		}
			
		return b;
	}

}
