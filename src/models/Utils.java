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
	 * @param neurons
	 * @return 4D position of the pattern 
	 */
	public static double[] patternPosition(Vector<INeuron> neurons){
		
		//calculate hypothetical position of pattern of "neurons" (average positions of x,y + some z)
		double[] position = {0,0,0,0};
		int ns = neurons.size();
		for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			double[] p = n.getPosition();
			position[0] += p[0];
			position[1] += p[1];
		}
		position[0] = position[0]/ns;
		position[1] = position[1]/ns;
		
		//calculate variances
		for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			double[] p = n.getPosition();
			position[2] += Math.pow(p[0]-position[0], 2);
			position[3] += Math.pow(p[1]-position[1], 2);
		}
		position[2] = position[2]/ns;
		position[3] = position[3]/ns;
		
		return position;
	}
	
	
	/**
	 * 
	 * @author lana
	 * Definition of "same pattern": same "position" variable.
	 * If 2 patterns have same x and y, smaller variances + smaller number of weights could be used for generalisation
	 * @param neurons list of neurons
	 * @param to_n destination
	 * @return vector of unique patterns that do not yet predict this neuron
	 */
	public static Vector<INeuron> patternExists3D(Vector<INeuron> neurons, INeuron to_n) {
		Vector<INeuron> valid_neurons = new Vector<INeuron>();
		//valid_neurons.addAll(neurons);
		double[] this_p = to_n.getPosition();

		//0: check if some of these neurons are equivalent
		//1: check if combined position of "neurons" already equivalent to to_n
		//2: check if we already have an inweight from a pattern neuron at that position
		
		for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			
			boolean add = true;
			for (Iterator<INeuron> iterator2 = valid_neurons.iterator(); iterator2.hasNext();) {
				INeuron n2 = iterator2.next();
				if(n.getId() != n2.getId()){
					double[] p1 = n.getPosition();
					double[] p2 = n2.getPosition();
					
					if(p1[0] == p2[0] && p1[1] == p2[1] && 
							p1[2] == p2[2] && p1[3] == p2[3]){
						add = false;
					}
				}
			}
			
			if(add){
				valid_neurons.add(n);
			}
		}

		//if single neuron
		if(valid_neurons.size()==1){
			//no direct iw with only 1 neuron
			/*mlog.say("no direct iw with only 1 neuron: " + valid_neurons.get(0).getId());
			
			mlog.say("original vector:");
			for (Iterator<INeuron> iterator2 = neurons.iterator(); iterator2.hasNext();) {
				INeuron n2 = iterator2.next();
				double[] p = n2.getPosition();
				mlog.say(""+ n2.getId() + " pos " + p[0] + " " + p[1] + " " + p[2] + " " + p[3]);
			}*/
			return new Vector<INeuron>();
		}
		
		//calculate hypothetical position of pattern of "neurons" (average positions of x,y + some z)
		double[] pos = patternPosition(valid_neurons);
		
		//mlog.say("pattern position " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3]);
		
		//is that already our position?
		//(this neuron is a pattern neuron with "neurons" as input pattern already)
		if(this_p[0] == pos[0] && this_p[1] == pos[1] && 
				this_p[2] == pos[2] && this_p[3] == pos[3]){
			//mlog.say("this neuron is a pattern neuron with this as input pattern already");
			return new Vector<INeuron>();
		}
		
		
		//is that the position of one of our inputs already?
		Set<INeuron> from_neurons =  to_n.getInWeights().keySet();
		for (Iterator<INeuron> iterator = from_neurons.iterator(); iterator.hasNext();) {
			INeuron iNeuron = iterator.next();
			this_p = iNeuron.getPosition();
			if(this_p[0] == pos[0] && this_p[1] == pos[1] && 
					this_p[2] == pos[2] && this_p[3] == pos[3]){
				//mlog.say("that is the position of one of our inputs already");
				return new Vector<INeuron>();
			}
		}
		
		return valid_neurons;
	}

	
	/**
	 * 
	 * @author lana
	 * @param neurons list of neurons
	 * @param to_n destination
	 * @param valid_neurons (to avoid counting sensors as neurons)
	 * @return true if there exists a PNeuron in the whole net that can be activated by the "neurons" pattern
	 * and has an outweight to to_n
	 */
	/*public static boolean patternExists(Vector<INeuron> neurons, INeuron to_n, Collection<INeuron> valid_neurons) {
		boolean b = false;
		
		if(neurons.size()==1 && neurons.get(0)==to_n){
			return true;
		}
		
		//input neurons
		Set<INeuron> from_neurons =  to_n.getInWeights().keySet();
		//collection of patterns predicting this neuron
		//to check if "neurons" are not just equivalent to patterns we already have
		Vector<Set<INeuron>> cp = new Vector<Set<INeuron>>();
		//list of neurons having unique patterns
		Vector<INeuron> original_n = new Vector<INeuron>();

		//look at each pattern going to this neuron
		//check if "neurons" is equal to any of the patterns
		for (Iterator<INeuron> iterator = from_neurons.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			//mlog.say("id n "+ n.getId());

			//direct in weights to input neurons
			Vector<BundleWeight> bws = n.getDirectInWeights();
			for (Iterator<BundleWeight> iterator2 = bws.iterator(); iterator2.hasNext();) {
				BundleWeight bw = iterator2.next();
				
				//pattern for direct in weight
				Set<INeuron> set = bw.getInNeurons();
				
				if(set.equals(neurons)){
					return true;
				}
				
				//check if set already exists
				boolean exists = false;
				for (Iterator<Set<INeuron>> iterator3 = cp.iterator(); iterator3.hasNext();) {
					Set<INeuron> pattern = iterator3.next();
					
					if(set.equals(pattern) || !valid_neurons.containsAll(set)){
						exists = true;
					}
				}
				
				if(!exists){
					cp.addElement(set);
					if(!original_n.contains(n)){
						original_n.addElement(n);
						mlog.say("added "+n.getId());
					}
				}
			}
		}
		
		//mlog.say("cp size " + cp.size());
		
		if(cp.size()==0){
			//?
		}
		
		//check if the list of "original neurons" is different from "neurons"
		if(original_n.equals(neurons)){
			return true;
		}else{
			mlog.say("not equal");
			for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				mlog.say(" " + n.getId());
			}
			mlog.say("and ");
			for (Iterator<INeuron> iterator = original_n.iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				mlog.say(" " + n.getId());
			}
		}

		//next check if there is a new pattern in the proposed neurons (or TODO remove similar patterns)
		if(cp.size()==1){
			Set<INeuron> s = cp.get(0);
			if(s.containsAll(neurons)){
				return true;
			}
			
			
			boolean bad2 = true;
			for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				Vector<BundleWeight> bws = n.getDirectInWeights();
				for (Iterator iterator2 = bws.iterator(); iterator2.hasNext();) {
					BundleWeight bw = (BundleWeight) iterator2.next();
					Set<INeuron> set = bw.getInNeurons();
					if(!set.equals(s)){
						bad2 = false;
					}
				}
			}
			if(bad2){
				b = true;
			}
		}
	
		return b;
	}*/

	
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
	 * also unmutes them.
	 * @param layer map of neurons to be reset.
	 */
	public static void resetNeuronsActivation(Collection<INeuron> layer){
		Iterator<INeuron> it = layer.iterator();
		while(it.hasNext()){
			INeuron n = it.next();
			n.resetActivation();
			n.setMute(false);
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
	 * TODO recalculate positions of neurons after snapping
	 * */
	public static HashMap<Integer, INeuron> snap(HashMap<Integer, INeuron> allINeurons) {
		mlog.say("snapping");
		int nw = countWeights(allINeurons);
		mlog.say("total connections "+ nw + " neurons "+ allINeurons.size());		
		
		ArrayList<INeuron> remove = new ArrayList<INeuron>();
		//go through net
		Iterator<Entry<Integer, INeuron>> it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			
			//mlog.say("here");
			
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			
			boolean doit = true;
			
			if(!n.justSnapped && doit){ 
				//mlog.say("here 1");

				//look for equivalent neurons (neurons with equivalent outweights)
				Iterator<Entry<Integer, INeuron>> it2 = allINeurons.entrySet().iterator();
				while(it2.hasNext()){
					Map.Entry<Integer, INeuron> pair2 = it2.next();
					INeuron n2 = pair2.getValue();
										
					if((n.getId() != n2.getId()) && !n2.justSnapped && doit){
						//mlog.say("here 2");
						
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
								!s1.equals(s2)){
							//mlog.say("too different");
							dosnap = false;
						} else {
							//mlog.say("compare outw");
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
								n.recalculatePosition();
			
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
	

	/**
	 * recursively propagates direct activation
	 * @param collection
	 */
	public static void propagateInstantaneousActivation(Collection<INeuron> collection) {
		for (Iterator<INeuron> iterator = collection.iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			if(n.getActivation()>0){
				n.activateDirectOutWeights();
			}
		}
	}
}
