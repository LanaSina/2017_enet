package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import communication.Constants;

import java.util.Vector;

/**
 * Pattern neuron: only activated if all inweights are activated
 * I have a feeling we don't need this class.
 * @author lana
 *
 */
public class PNeuron extends INeuron {
	
	public PNeuron(int id) {
		super(id);
		init();
	}
	
	/**
	 * 
	 * @param stm list of pre-neurons
	 * @param n neuron to make the bundleWeigth to
	 */
	public PNeuron(Vector<INeuron> from, INeuron to, int id) {
		super(id);
		BundleWeight b = new BundleWeight(from, to);
		//TODO no the inweights are from a bunch of neurons
		//directInWeights.put(to, b);
		bundleWeights.put(b, from);
	
		//make probaweight
		ProbaWeight p = new ProbaWeight(Constants.defaultConnection);
		outWeights.put(to, p);
		init();
	}

	private void init() {
		mlog.setName("Pattern Neuron");
		//hasBundleWeights = true;
	}
	
	/*@Override
	public void makeDirectActivation() {
		Iterator<Entry<INeuron, ProbaWeight>> it = directInWeights.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = it.next();
			ProbaWeight w = pair.getValue();
			if(w.isActivated()){
				this.increaseActivation(1);
			}
		}				
	}*/


	/**
	 * calculates the probabilistic activation of this neuron
	 * and compares it to its direct activation.
	 * if there is not predicted activation but we are activated, the neuron should be "surprised"*/
	@Override
	public void calculateActivation() {
		super.calculateActivation();
		if(isActivated()){
			//mute down neurons TODO for normal neurons with sensory inweights too
			Iterator<Entry<INeuron, ProbaWeight>> it = directInWeights.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<INeuron, ProbaWeight> pair = it.next();
				ProbaWeight w = pair.getValue();
				w.muteInputNeurons();
			}
		}
	}
	
	/**
	 * @return true if one of the inweights is a bundleweights with these in-neurons
	 */
	@Override
	public boolean sameBundleWeights(Vector<INeuron> neurons, INeuron to_n) {
		boolean b = false;
		//iterate over bundleweights
		/*for (Iterator<Entry<INeuron, ProbaWeight>> iterator = directInWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight p = pair.getValue();
			if(p.sameBundle(neurons)){
				b = true;
				break;
			}
		}*/
		
		for (Iterator<Vector<INeuron>> iterator = bundleWeights.values().iterator(); iterator.hasNext();) {
			Vector<INeuron> v = iterator.next();
			if(v.containsAll(neurons) && neurons.containsAll(v)){//exactly equal
				if(outWeights.containsKey(to_n)){
					b = true;
					break;
				}
			}
		}
		return b;
	}
	
	
	@Override
	public boolean addBundleWeight(BundleWeight bw) {
		boolean b = false;
		if(!sameBundleWeight(bw)){
			
			b = true;
		}
		return b;
	}

	/**
	 * 
	 * @param bw
	 * @return true if we possess a similar bundleweight
	 */
	private boolean sameBundleWeight(BundleWeight bw) {
		boolean b = false;
		for (Iterator<BundleWeight> iterator = bundleWeights.keySet().iterator(); iterator.hasNext();) {
			BundleWeight ownbw = iterator.next();
			if(ownbw.sameBundle(bw.getInNeurons())){
				b = true;
				break;
			}
		}
		return b;
	}
	
}
