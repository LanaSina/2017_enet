package neurons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
		directInWeights.put(to, b);
		init();
	}

	private void init() {
		mlog.setName("Pattern Neuron");
		hasBundleWeights = true;
	}

	/**
	 * calculates the probabilistic activation of this neuron
	 * and compares it to its direct activation.
	 * if there is not predicted activation but we are activated, the neuron should be "surprised"*/
	@Override
	public void calculateActivation() {
		mlog.say("Pattern neuron activated");
		super.calculateActivation();
	}
	
	/**
	 * @return true if one of the inweights is a bundleweights with these in-neurons
	 */
	@Override
	public boolean sameBundleWeights(Vector<INeuron> neurons) {
		boolean b = false;
		//iterate over bundleweights
		for (Iterator<Entry<INeuron, ProbaWeight>> iterator = directInWeights.entrySet().iterator(); iterator.hasNext();) {
			Entry<INeuron, ProbaWeight> pair = iterator.next();
			ProbaWeight p = pair.getValue();
			if(p.sameBundle(neurons)){
				b = true;
				break;
			}
		}
		return b;
	}
	
}
