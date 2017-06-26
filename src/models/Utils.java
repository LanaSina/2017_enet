package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
		//collection of patterns predicting this neuron
		//to check if "neurons" are not just equivalent to patterns we already have
		Vector<Set<INeuron>> cp = new Vector<Set<INeuron>>();

		//look at each pattern going to this neuron
		//check if "neurons" is equal to any of the patterns
		for (Iterator<INeuron> iterator = from_neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			mlog.say("id n "+ n.getId());

			//direct in weights to input neurons
			Vector<BundleWeight> bws = n.getDirectInWeights();
			for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
				BundleWeight bw = iterator2.next();
				
				//pattern for direct in weight
				Set<INeuron> set = bw.getInNeurons();
				
				if(set.containsAll(neurons) && neurons.containsAll(set)){
					b = true;
				}
				
				//check if set already exists
				boolean exists = false;
				for (Iterator<Set<INeuron>> iterator3 = cp.iterator(); iterator3.hasNext();) {
					Set<INeuron> pattern = iterator3.next();
					if((pattern.containsAll(set) && set.containsAll(pattern)) || !valid_neurons.containsAll(set)){
						exists = true;
					}
				}
				
				if(!exists){
					cp.addElement(set);
					mlog.say("set ");
					for (Iterator iterator3 = set.iterator(); iterator3.hasNext();) {
						INeuron iNeuron = (INeuron) iterator3.next();
						mlog.say("set, id " + iNeuron.getId());
					}
					mlog.say("---------- " );
				}
			}
		}
		
		mlog.say("cp size " + cp.size());
		
		if(cp.size()==0){
			//?
		}
		
		//next check if there is a new pattern in the proposed neurons (or TODO remove similar patterns)
		if(cp.size()==1){
			Set<INeuron> s = cp.get(0);
			if(s.containsAll(neurons)){
				b = true;
			}
			
			boolean bad = true;
			for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				Vector<BundleWeight> bws = n.getDirectInWeights();
				for (Iterator iterator2 = bws.iterator(); iterator2.hasNext();) {
					BundleWeight bw = (BundleWeight) iterator2.next();
					Set<INeuron> set = bw.getInNeurons();
					if(!(set.containsAll(s) && s.containsAll(set))){
						bad = false;
					}
				}
			}
			if(bad){
				b = true;
			}
		}
		
		
			
		//go through potential input and store each unique pattern
		//if there is only one pattern
		/*for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			mlog.say("id n "+ n.getId());
			
			Vector<BundleWeight> bws = n.getDirectInWeights();
			//origin neuron
			if(bws.size()==0){
				Vector<INeuron> a = new Vector<INeuron>();
				a.addElement(n);
				cp.add(new HashSet<INeuron>(a));
				
				mlog.say("added to cp id "+ n.getId());
			} 
			
			for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
				BundleWeight bw = iterator2.next();
				Set<INeuron> set = bw.getInNeurons();
				
				for (Iterator iterator3 = set.iterator(); iterator3.hasNext();) {
					INeuron iNeuron = (INeuron) iterator3.next();
					mlog.say("set, id " + iNeuron.getId());
				}
				mlog.say("---------- " );
				
				//check if set already exists
				boolean exists = false;
				for (Iterator<Set<INeuron>> iterator3 = cp.iterator(); iterator3.hasNext();) {
					Set<INeuron> pattern = iterator3.next();
					if(pattern.containsAll(set)){
						exists = true;
					}
				}
				
				if(!exists){
					cp.addElement(set);
					mlog.say("added set ");
				}
			}
		}
		
		//there is only one valid pattern and it already had been activated
		if(cp.size()<2){
			b = true;
		}
		
		for (Iterator iterator = cp.iterator(); iterator.hasNext();) {
			Set<INeuron> set = (Set<INeuron>) iterator.next();
			for (Iterator iterator2 = set.iterator(); iterator2.hasNext();) {
				INeuron iNeuron = (INeuron) iterator2.next();
				mlog.say("id " + iNeuron.getId());
			}
			mlog.say("---------- " );
		}*/
		
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
	

	/**
	 * calculates surprise and activate direct outweights
	 * @param neurons
	 */
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
