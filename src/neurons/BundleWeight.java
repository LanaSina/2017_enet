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
 * @author lana
 *
 */
public class BundleWeight extends ProbaWeight {
	
	/** the inweights*/
	HashMap<INeuron, ProbaWeight> bundle = new HashMap<>();

	/**
	 * This constructor might never be used
	 * @param type 
	 */
	public BundleWeight(int type) {
		super(type);
	}
	
	/**
	 * 
	 * @param from
	 * @param to not actually used
	 */
	public BundleWeight(Vector<INeuron> from, INeuron to) {
		//create age and value
		super(Constants.defaultConnection);
		
		//create bundle
		for (Iterator<INeuron> iterator = from.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			ProbaWeight p = new ProbaWeight(Constants.fixedConnection);
			//TODO I think we don't need to link to an actual neuron. DirectOutWeigths don't need to know who's out, do they
			n.addDirectOutWeight(p, to);
			bundle.put(n, p);
		}
	}
	
	/**
	 * @return true if all weights in bundle are activated, false otherwise
	 */
	@Override
	public boolean isActivated() {
		boolean b = true;
		for (Iterator<INeuron> iterator = bundle.keySet().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			if(!n.isActivated()){
				b = false;
				break;
			}else{
				//mlog.say("bundle strand activated "+n.getId());
			}
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

}
