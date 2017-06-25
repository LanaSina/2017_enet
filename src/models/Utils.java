package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import communication.Constants;
import communication.MyLog;
import neurons.BundleWeight;
import neurons.INeuron;
import neurons.ProbaWeight;

public class Utils {
	static MyLog mlog = new MyLog("Utils", true);
	
	/**
	 * 
	 * @author lana
	 * @param neurons list of neurons
	 * @param to_n destination
	 * @param valid_neurons (to avoid counting sensors as neurons)
	 * @return true if there exists a PNeuron in the whole net that can be activated by the "neurons" pattern
	 * and has an outweight to to_n
	 */
	public static boolean patternExists(Vector<INeuron> neurons, INeuron to_n, Collection<INeuron> valid_neurons) {
		boolean b = false;
		
		//input neurons
		Set<INeuron> from_neurons =  to_n.getInWeights().keySet();
		//collection of patterns
		Vector<Set<INeuron>> cp = new Vector<Set<INeuron>>();
		for (Iterator<INeuron> iterator = from_neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			//direct in weights to input neurons
			Vector<BundleWeight> bws = n.getDirectInWeights();
			for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
				BundleWeight bw = iterator2.next();
				//pattern for direct in weight
				Set<INeuron> set = bw.getInNeurons();
				//mlog.say("--------- ");
				if(set.containsAll(neurons)){
					b = true;
					//mlog.say("############ B");
				}
				
				//check if set already exists
				boolean exists = false;
				for (Iterator<Set<INeuron>> iterator3 = cp.iterator(); iterator3.hasNext();) {
					Set<INeuron> pattern = iterator3.next();
					if(pattern.containsAll(set) || !valid_neurons.containsAll(set)){
						exists = true;
						//mlog.say("############ A");
					}
				}
				
				if(!exists){
					cp.addElement(set);
				}
			}
		}
		/*mlog.say("############ cp size "+ cp.size());
		if(cp.size()==81){
			for (Iterator<Set<INeuron>> iterator3 = cp.iterator(); iterator3.hasNext();) {
				Set<INeuron> s =  iterator3.next();
				for (Iterator iterator = s.iterator(); iterator.hasNext();) {
					INeuron iNeuron = (INeuron) iterator.next();
					mlog.say("############ " + iNeuron.getId());
				}
				mlog.say("--------- ");
			}
		}*/
		//there is only one valid pattern and it already had been activated
		if(cp.size()<2){
			b = true;
			
			
		}
		return b;
	}

	
	/**
	 * for all neurons in this layer calculate probabilistic activation
	 * then activate all outside weights 
	 * if the neuron activation is above a threshold 
	 * @param layer
	 */
	public static void activateOutWeights(HashMap<Integer, INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = pair.getValue();
			if(ne.isActivated()){
				ne.activateOutWeights();
			}
		}
	}
	
	/**
	 * resets output weights activation to 0 in this layer
	 * @param layer
	 */
	public static void resetOutWeights(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = pair.getValue();
			ne.resetOutWeights();
		}
	}
	
	/**
	 * reset direct output weights activation to 0 in this layer
	 * @param layer
	 */
	public static void resetDirectOutWeights(HashMap<Integer,INeuron> layer){
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = (INeuron) pair.getValue();
			ne.resetDirectOutWeights();
		}
	}
	
	
	/**
	 * resets neurons activation to 0
	 * @param layer map of neurons to be reset.
	 */
	public static void resetNeuronsActivation(Collection<INeuron> layer){
		Iterator<INeuron> it = layer.iterator();
		while(it.hasNext()){
			INeuron n = it.next();
			n.resetActivation();
		}
	}
	
	
	/**
	 * age the outweights of neurons that are currently activated
	 */
	public static void ageOutWeights(HashMap<Integer, INeuron> layer) {
		Iterator<INeuron> it = layer.values().iterator();
		while(it.hasNext()){
			INeuron n =  it.next();
			if(n.getActivation()>0){
				n.ageOutWeights();
			}
		}
	}
	

	public static void calculateAndPropagateActivation(HashMap<Integer, INeuron> neurons) {
		for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			n.calculateActivation();
			n.propagateActivation();
		}
	}
	
	/**
	 * increase the inweights of neurons that are currently activated
	 */
	public static void increaseInWeights(HashMap<Integer, INeuron> layer) {
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.getActivation()>0){
				n.increaseInWeights();
			}
		}
	}

	
	/** 
	 * fuses similar neurons
	 * */
	public static HashMap<Integer, INeuron> snap(HashMap<Integer, INeuron> allINeurons) {
		mlog.say("snapping");
		int nw = countWeights(allINeurons);
		mlog.say("total connections "+ nw + " neurons "+ allINeurons.size());		
		
		ArrayList<INeuron> remove = new ArrayList<INeuron>();
		//go through net
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			
			boolean doit = true;
			/*Vector<BundleWeight> aaa = n.getDirectInWeights();
			for (Iterator iterator = aaa.iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = (BundleWeight) iterator.next();
				if(bundleWeight.getInNeurons().size()>1){
					doit = false;
					break;
				}
			}*/
			
			if(!n.justSnapped && doit){ 

				//look for equivalent neurons (neurons with equivalent outweights)
				Iterator<Entry<Integer, INeuron>> it2 = allINeurons.entrySet().iterator();
				while(it2.hasNext()){
					Map.Entry<Integer, INeuron> pair2 = it2.next();
					INeuron n2 = pair2.getValue();
					
					/*Vector<BundleWeight> bbb = n2.getDirectInWeights();
					for (Iterator iterator = bbb.iterator(); iterator.hasNext();) {
						BundleWeight bundleWeight = (BundleWeight) iterator.next();
						if(bundleWeight.getInNeurons().size()>1){
							doit = false;
							break;
						}
					}*/
					
					//mlog.say(" " + n.getId() + " " + n2.getId() + " " + n2.justSnapped);

										
					if((n.getId() != n2.getId()) && !n2.justSnapped && doit){
						
						
						boolean dosnap = true;

						//compare all out weights
						HashMap<INeuron,ProbaWeight> out1 = n.getOutWeights();
						HashMap<INeuron,ProbaWeight> out2 = n2.getOutWeights();
						Iterator<Entry<INeuron, ProbaWeight>> out2it = out2.entrySet().iterator();
						//n1 must have all the weights that n2 has
						Set<INeuron> s1 = out1.keySet();
						Set<INeuron> s2 = out2.keySet();
						//inweights
						HashMap<INeuron,ProbaWeight> in1 = n.getInWeights();
						HashMap<INeuron,ProbaWeight> in2 = n2.getInWeights();
						
						
						//avoid direct recurrent connections
						if(n.directInWeightsContains(n2) || n2.directInWeightsContains(n) ||
								//avoid different sets of outweights
								!s1.containsAll(s2) || !s2.containsAll(s1)){
							//mlog.say("too different");
							dosnap = false;
						} else {
							//compare outw
							while(out2it.hasNext()){
								Map.Entry<INeuron, ProbaWeight> out2pair = out2it.next();
								ProbaWeight w2 = out2pair.getValue();
								//can still learn: give up
								if(w2.canLearn()){
									//mlog.say("can learn");
									dosnap = false;
									break;
								}
								
								//weight to same neuron; check value
								ProbaWeight w1 = out1.get(out2pair.getKey());
								if(w1.canLearn()){
									//mlog.say("can learn");
									dosnap = false;
									break;//give up
								}
								
								if(Math.abs(w1.getProba()-w2.getProba())>Constants.w_error){
									//mlog.say("wrong out value");
									dosnap = false;
									break;
								};
								
								
								//finally, only snap if there are no conflicting inweights
								Iterator<Entry<INeuron, ProbaWeight>> in1it = in1.entrySet().iterator();
								while(in1it.hasNext()){
									Map.Entry<INeuron, ProbaWeight> entry = in1it.next();
									INeuron c = entry.getKey();
									if(in2.containsKey(c)){
										ProbaWeight p1 = entry.getValue();
										ProbaWeight p2 = in2.get(c);
										if(Math.abs(p1.getProba()-p2.getProba())>Constants.w_error){
											//mlog.say("wrong in value");
											dosnap = false;
											break;
										}
									}/* else {
										dosnap = false;
										//a bit sad about this but causes strong illusions
									}*/
								}
							}
								
							if(dosnap){
								n.justSnapped = true;
								n2.justSnapped = true;
								remove.add(n2);
								
								//report n2 inputs to n if they did not exist
								//todo error here
								n2.reportInWeights(n);
								
								//do the same for direct inweights
								n2.reportDirectInWeights(n);
			
								//now report direct outweights
								n2.reportDirectOutWeights(n);
								
								//notifies output neurons too
								n2.removeAllOutWeights();
								
								n2.clearDirectInWeights();									
							}
														
						}
					}
				}
			}
		}
		
		//count removed weights (only out weights)
		for(int i=0; i<remove.size();i++){	
			allINeurons.remove(remove.get(i).getId());
		}
		
		
		//reset "just snapped" values and remove ghost outweights
		it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.justSnapped = false;
		}
		
		nw = countWeights(allINeurons);
		mlog.say("after: weights "+ nw + " neurons " + allINeurons.size());
		
		return allINeurons;
	}
	
	public static int countWeights(HashMap<Integer, INeuron> allINeurons) {
		int nw = 0;
		for (Iterator<INeuron> iterator = allINeurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			nw+= n.getOutWeights().size();
		}
		return nw;
	}
}
