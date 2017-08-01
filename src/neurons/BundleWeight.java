package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.Set;
import java.util.Vector;

import communication.Constants;

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
		mlog.setName("BWeight");
	}
	
	/**
	 * 
	 * @param from
	 * @param to not actually used
	 */
	public BundleWeight(Vector<INeuron> from, INeuron to) {		
		//create age and value
		super(Constants.defaultConnection);
		mlog.setName("BWeight");
		
		//create bundle
		for (Iterator<INeuron> iterator = from.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			ProbaWeight p = new ProbaWeight(Constants.defaultConnection);
			p.setValue(1);
			bundle.put(n, p);
			n.addDirectOutWeight(to, this);
		}
	}
	
	/**
	 * @param p on direct inweights: mean(x), mean (y), var(x), var(y))
	 */
	public void setPosition(double[] p) {
		position = p.clone();
	}
	
	public double[] getPosition() {
		return position.clone();
	}

	
	/**
	 * @return true if all weights in bundle are activated, false otherwise
	 */
	public boolean bundleIsActivated() {
		/*boolean b = true;
		for (Iterator<INeuron> iterator = bundle.keySet().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			if(!n.isActivated()){
				b = false;
				break;
			}
		}//*/
		
		boolean b = false;
		//>90% of 0.9 weights must be activated
		//>70% of 0.7 weights etc
		int size = bundle.size();
		int activated = 0;
		double sum = 0; 
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = bundle.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight pw = pair.getValue();
			if(pw.isActivated()){
				activated++;
				sum+=pw.getProba();
			}
		}
		
		double mean = sum/activated;
		double threshold = 1-mean;
		double t = activated*1.0/size;
		if(t>=threshold){
			b = true;
			//mlog.say("true");
		}
		
		return b;
	}
	
	
	/**
	 * @return true if bundle is the same
	 */
	@Override
	public boolean sameBundle(Set<INeuron> neurons) {
		boolean b = true;
		//are the 2 sets completely equivalent
		if(!neurons.containsAll(bundle.keySet())){
			b = false;
		}else if(!bundle.keySet().containsAll(neurons)){
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
			mlog.say("********** empty");
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
		return (HashMap<INeuron, ProbaWeight>) bundle.clone();
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
	
	@Override
	public void resetActivation() {
		for (Iterator<ProbaWeight> iterator = bundle.values().iterator(); iterator.hasNext();) {
			ProbaWeight p = iterator.next();
			p.resetActivation();
		}		
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
	 * decrease probabilities all neurons that are not in newBundle
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

}
