package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import communication.Constants;
import communication.MyLog;
import neurons.INeuron;
import neurons.ProbaWeight;

public class Utils {
	static MyLog mlog = new MyLog("Utils", true);
	
	
	/** 
	 * fuses similar neurons
	 * */
	private static void snap(HashMap<Integer, INeuron> allINeurons) {
		mlog.say("snapping");
		int nw = countWeights();
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
							dosnap = false;
						} else {
							//compare outw
							while(out2it.hasNext()){
								Map.Entry<INeuron, ProbaWeight> out2pair = out2it.next();
								ProbaWeight w2 = out2pair.getValue();
								//can still learn: give up
								if(w2.canLearn()){
									dosnap = false;
									break;
								}
								
								//weight to same neuron; check value
								ProbaWeight w1 = out1.get(out2pair.getKey());
								if(w1.canLearn()){
									dosnap = false;
									break;//give up
								}
								
								if(Math.abs(w1.getProba()-w2.getProba())>Constants.w_error){
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
											dosnap = false;
											break;
										}
									}
								}
							}
								
							if(dosnap){
								n.justSnapped = true;
								n2.justSnapped = true;
								remove.add(n2);
								
								//report n2 inputs to n if they did not exist
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
		
		nw = countWeights();
		mlog.say("after: weights "+ nw + " neurons " + allINeurons.size());
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
