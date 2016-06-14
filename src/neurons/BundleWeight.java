package neurons;

import java.util.Iterator;
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
	Vector<ProbaWeight> bundle = new Vector<ProbaWeight>();

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
			bundle.addElement(p);
		}
	}
	
	/**
	 * @return true if all weights in bundle are activated, false otherwise
	 */
	@Override
	public boolean isActivated() {
		boolean b = true;
		for (Iterator<ProbaWeight> iterator = bundle.iterator(); iterator.hasNext();) {
			ProbaWeight p = iterator.next();
			if(!p.isActivated()){
				b = false;
			}
		}
		return b;
	}
}
