package communication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

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
			
			if(n.isActivated()){
				n.ageOutWeights();
				
			}
			
		}
	}
	

	/**
	 * activate direct outweights recursively
	 * @param neurons
	 */
	public static void calculateAndPropagateActivation(HashMap<Integer, INeuron> neurons) {
		for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
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
			if(n.isActivated()){
				n.increaseInWeights();
			}
		}
	}
		

	/** 
	 * fuses similar neurons
	 * */
	public static HashMap<Integer, INeuron> snap( HashMap<Integer, INeuron> allINeurons) {
		mlog.say("snapping");
		int nw = countWeights(allINeurons);
		mlog.say("total connections "+ nw + " neurons "+ allINeurons.size());		
		
		//use a modifiable list
		HashMap<Integer, INeuron> neurons = (HashMap<Integer, INeuron>) allINeurons.clone();
		
		ArrayList<INeuron> remove = new ArrayList<INeuron>();
		ArrayList<INeuron> changed = new ArrayList<INeuron>();

		int snapped = 0;
		int removed_young_w = 0;
		int deleted  = 0;
		
		//go through net
		Iterator<Entry<Integer, INeuron>> it = neurons.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			
			if(remove.contains(n)){
				continue;
			}

			
			removed_young_w+=removeYoungWeights(n);
			//remove pattern neurons that have no function
			//might need unsnapping later...
			/*if(n.getOutWeights().size()==0 && n.getDirectOutWeights().size()==0){
				deleted++;
				remove.add(n);
				
				n.removeAllOutWeights();
				n.removeAllInWeights();
				n.clearDirectInWeights();									
				continue;
			}*/
		}
		
		it = neurons.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			
			if(remove.contains(n)){
				continue;
			}
			
			boolean doit = true;
			boolean dosnap = true;
			
			if(!n.justSnapped && doit){ 
				//avoid co-weigths that still can learn
				HashMap<INeuron,ProbaWeight> co_w = n.getCoWeights();
				Iterator<Entry<INeuron, ProbaWeight>> co_it = co_w.entrySet().iterator();
				while (co_it.hasNext()) {
					Entry<INeuron, ProbaWeight> pair3 = co_it.next();
					ProbaWeight w = pair3.getValue();
					if(w.canLearn()){
						dosnap = false;
						break;
					}
				}
				
				if(!dosnap){
					//no need to consider it for future comparisons
					it.remove();
					continue;
				}//*/
				
				//prune direct weights and snap those with same position
				//TODO
				
				//look for equivalent neurons (neurons with equivalent outweights)
				Iterator<Entry<Integer, INeuron>> it2 = neurons.entrySet().iterator();
				while(it2.hasNext()){
					dosnap = true;
					
					Map.Entry<Integer, INeuron> pair2 = it2.next();
					INeuron n2 = pair2.getValue();

					//avoid co-weigths that still can learn
					HashMap<INeuron,ProbaWeight> co_w2 = n2.getCoWeights();
					Iterator<Entry<INeuron, ProbaWeight>> co_it2 = co_w2.entrySet().iterator();
					while (co_it2.hasNext()) {
						Entry<INeuron, ProbaWeight> pair3 = co_it2.next();
						ProbaWeight w = pair3.getValue();
						if(w.canLearn()){
							dosnap = false;
							break;
						}
					}//*/
					
					if(!dosnap){
						continue;
					}
										

					if((n.getId() != n2.getId()) && !n2.justSnapped && doit){

						//compare all out weights
						HashMap<INeuron,ProbaWeight> out1 = n.getOutWeights();
						HashMap<INeuron,ProbaWeight> out2 = n2.getOutWeights();

						//inweights
						HashMap<INeuron,ProbaWeight> in1 = n.getInWeights();
						HashMap<INeuron,ProbaWeight> in2 = n2.getInWeights();
						Set<INeuron> i1 = in1.keySet();
						Set<INeuron> i2 = in2.keySet();
						
						//avoid direct recurrent connections
						/*if(//this should be ok n.directInWeightsContains(n2) || n2.directInWeightsContains(n) ||
								//avoid different sets ofinweights
								  !i1.equals(i2)
								 ){ 
							//mlog.say("too different");
							dosnap = false;
							continue;
						} */
							
						dosnap = sameWeights(out1, out2);

						if(!dosnap){
							continue;
						}
						
						/*dosnap = sameWeights(in1, in2);
						if(!dosnap){
							continue;
						}*/
						
						//no learning inweights either
						Iterator<Entry<INeuron, ProbaWeight>> in1it = in1.entrySet().iterator();
						while (in1it.hasNext()) {
							Entry<INeuron, ProbaWeight> pa = in1it.next();
							if(pa.getValue().canLearn()){
								dosnap = false;
								break;
							}
						}
						if(!dosnap){
							continue;
						}
						
						Iterator<Entry<INeuron, ProbaWeight>> in2it = in2.entrySet().iterator();
						while (in2it.hasNext()) {
							Entry<INeuron, ProbaWeight> pa = in2it.next();
							if(pa.getValue().canLearn()){
								dosnap = false;
								break;
							}
						}
						if(!dosnap){
							continue;
						}//*/
							
						//finally, only snap if there are no conflicting inweights
						/*Iterator<Entry<INeuron, ProbaWeight>> in1it = in1.entrySet().iterator();
						while(in1it.hasNext()){
							Map.Entry<INeuron, ProbaWeight> entry = in1it.next();
							INeuron c = entry.getKey();
							if(in2.containsKey(c)){
								ProbaWeight p1 = entry.getValue();
								ProbaWeight p2 = in2.get(c);
								if(p1.canLearn() || p2.canLearn()){
									dosnap = false;
									break;
								}
									
								if(Math.abs(p1.getProba()-p2.getProba())>Constants.w_error){
									//mlog.say("wrong in value");
									dosnap = false;
									break;
								}
							} else {
								dosnap = false;
								break;
								//a bit sad about this but causes strong illusions
							}
						}//*/
						
						if(dosnap){
							snapped++;
							//n.justSnapped = true;
							n2.justSnapped = true;
							remove.add(n2);
							
							if(n.getId()==7431 ||  n.getId()==7123 || n2.getId()==7431 ||  n2.getId()==7123){
								mlog.say("snapping " + n.getId() + " " + n2.getId());
							}
							//report n2 inputs to n if they did not exist
							n2.reportInWeights(n);
							n2.reportCoWeights(n);
							changed.add(n);
							
							//do the same for direct inweights
							n2.reportDirectInWeights(n);
							//now report direct outweights and remaps too
							n2.reportDirectOutWeights(n);
							
							
							//notifies output neurons too
							n2.removeAllOutWeights();
							n2.clearDirectInWeights();		
						}
					}
				}
			}
			
			it.remove();
		}
		mlog.say("removed " + removed_young_w + " youngs; snapped "+ snapped + " deleted " + deleted);
		//count removed weights (only out weights)
		for(int i=0; i<remove.size();i++){	
			int id = remove.get(i).getId();
			//mlog.say("remove "+id);
			allINeurons.remove(id);
		}
		
		for(int i=0; i<changed.size();i++){	
			INeuron n = changed.get(i);
			n.recalculatePosition();
		}//*/
		

		
		//reset "just snapped" values 
		it = allINeurons.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			n.justSnapped = false;
			//check in weights
			for (Iterator iterator = remove.iterator(); iterator.hasNext();) {
				INeuron iNeuron = (INeuron) iterator.next();
				if(n.getInWeights().get(iNeuron)!=null){
					throw new java.lang.Error("dead neuron in here");
				}
				if(n.getOutWeights().get(iNeuron)!=null){
					throw new java.lang.Error("dead neuron out here");
				}
			}
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
			if(n.isActivated()){
				n.activateDirectOutWeights();
			}
		}
	}
	
	public static void saveNet(HashMap<Integer, INeuron> net, Vector<INeuron> sensors, String folder, int step) {
		try {
			//first create directory
			String folderName = folder + "/" + step;
			File theDir = new File(folderName);
			// if the directory does not exist, create it
			if (!theDir.exists()) {
			    mlog.say("creating directory: " + folderName);
			    boolean result = false;
			    try{
			        theDir.mkdir();
			        result = true;
			    } 
			    catch(SecurityException se){
			    }        
			    if(result) {    
			        System.out.println("DIR created");  
			    }
			}
			
			//write sensors
			String sensors_file = folderName + "/" + Constants.Sensors_file_name;
			FileWriter sensor_writer = new FileWriter(sensors_file);
			String str = "sensor_ID, neuron_ID, x, y, sx, sy \n";
			sensor_writer.write(str);
			sensor_writer.flush();
			
			for (Iterator<INeuron> iterator = sensors.iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				INeuron to =  n.getDirectOutWeights().keySet().iterator().next();
				double[] pos = to.getPosition();
				str = n.getId() + "," + to.getId() + ","
					+ pos[0] + "," + pos[1] + ","+ pos[2] + "," + pos[3] +  "\n";//pos useless
				sensor_writer.write(str);
				sensor_writer.flush();
			}
			sensor_writer.close();
			
			//write neurons positions
			String pos_file = folderName+"/" + Constants.Positions_file_name;
			FileWriter pos_writer = new FileWriter(pos_file);
			str = "ID, x, y, sx, sy\n";
			pos_writer.write(str);
			pos_writer.flush();
			for (Iterator<INeuron> iterator = net.values().iterator(); iterator.hasNext();) {
				INeuron n = iterator.next();
				double[] pos = n.getPosition();
				str = n.getId() + "," + pos[0] + "," + pos[1] + ","+ pos[2] + "," + pos[3] +  "\n";
				//mlog.say("saved " + n.getId());
				pos_writer.write(str);
				pos_writer.flush();
			}
			
			pos_writer.close();
			
			//write net
			String net_file = folderName+"/" + Constants.Net_file_name;
			FileWriter net_writer = new FileWriter(net_file);
			str = "ID, weight_type, weight_id, in_neuron, value, age\n";
			net_writer.write(str);
			net_writer.flush();
			for (Iterator<Entry<Integer, INeuron>> iterator = net.entrySet().iterator(); iterator.hasNext();) {
				Entry<Integer, INeuron> pair = iterator.next();
				INeuron n = pair.getValue();
				Integer id = pair.getKey();
				
				//direct inweights 1st
				Iterator<BundleWeight> it_din = n.getDirectInWeights().iterator();
				int i = 0;
				while (it_din.hasNext()) {
					BundleWeight b = it_din.next();
					Iterator<Entry<INeuron, ProbaWeight>> it_b = b.getBundle().entrySet().iterator();
					while (it_b.hasNext()) {
						Entry<INeuron,ProbaWeight> entry = it_b.next();
						str = id + ",direct," + i + "," + entry.getKey().getId() + ",20,20\n";
						net_writer.write(str);
						net_writer.flush();
					}
					i++;
				}
				
				//proba weights
				Iterator<Entry<INeuron, ProbaWeight>> it_pin = n.getInWeights().entrySet().iterator();
				while (it_pin.hasNext()) {
					Entry<INeuron, ProbaWeight> entry = it_pin.next();
					ProbaWeight p = entry.getValue();
					INeuron in = entry.getKey();
					if(!net.containsKey(in.getId())){
						mlog.say("dead neuron");
						throw new java.lang.Error("dead neuron " + in.getId() + " to " + n.getId());
					}
					str = id + ",proba," + -1 + "," + entry.getKey().getId() + "," + p.getValue() + "," + p.getAge() + "\n";
					net_writer.write(str);
					net_writer.flush();
				}
			
			}
			
			net_writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mlog.say("Network Saved");
	}


	public static void updateCoactivation(INeuron n, HashMap<Integer, INeuron> allINeurons) {
		
		//age coactivation weights
		HashMap<INeuron,ProbaWeight> co_w = n.getCoWeights();
		Iterator<Entry<INeuron, ProbaWeight>> co_it = co_w.entrySet().iterator();
		while (co_it.hasNext()) {
			Entry<INeuron, ProbaWeight> pair = co_it.next();
			ProbaWeight w = pair.getValue();
			if(w.canLearn()){
				w.increaseAge();
				if(pair.getKey().isActivated()){
					w.addValue();
				}
			}
		}
		
		//increase values of input co-weights
		/*HashMap<INeuron,ProbaWeight> in_co_w = n.getInCoWeights();
		Iterator<Entry<INeuron, ProbaWeight>> i_co_it = in_co_w.entrySet().iterator();
		while (i_co_it.hasNext()) {
			Entry<INeuron, ProbaWeight> pair = i_co_it.next();
			ProbaWeight w = pair.getValue();
			if(w.canLearn()){
				w.addValue();
			}
		}*/
	}//*/
	
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	private static boolean sameWeights(HashMap<INeuron,ProbaWeight> a, HashMap<INeuron,ProbaWeight> b) {
		boolean same = true;
		
		//must contain same units
		if(!a.keySet().equals(b.keySet())){
			return false;
		}
		
		//compare outw
		Iterator<Entry<INeuron, ProbaWeight>> ai = a.entrySet().iterator();
		
		while(ai.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = ai.next();
			ProbaWeight w2 = pair.getValue();
			ProbaWeight w1 = b.get(pair.getKey());
			
			//can still learn: give up
			/*if(w2.canLearn()){
				return false;
			}*/
			/*if(w1.canLearn()){
				return false;
			}*/

			//weight to same neuron; check value
			if(Math.abs(w1.getProba()-w2.getProba())>Constants.w_error){
				return false;
			};
		}
		
		return same;
	}
	
	/**
	 * also remove age == 1 weights,
	 */
	private static int removeYoungWeights(INeuron n) {
		HashMap<INeuron, ProbaWeight> a = n.getOutWeights();
		int sum = 0;
		
		//remove age=1 out weights
		/*Iterator<Entry<INeuron, ProbaWeight>> ai = a.entrySet().iterator();
		while(ai.hasNext()){
			Map.Entry<INeuron, ProbaWeight> pair = ai.next();
			ProbaWeight w2 = pair.getValue();
			if(w2.getAge()==1){
				sum++;
				ai.remove();
				pair.getKey().removeInWeight(n);
			} 
		}*/
		
		//remove value=1 direct out weights
		Map<INeuron, ArrayList<BundleWeight>> b = n.getDirectOutWeights();
		Iterator<Entry<INeuron, ArrayList<BundleWeight>>> bi = b.entrySet().iterator();
		while (bi.hasNext()) {
			Entry<INeuron, ArrayList<BundleWeight>> pair = bi.next();
			for (Iterator<BundleWeight> iterator = pair.getValue().iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = iterator.next();
				bundleWeight.removeYoungWeigths(pair.getKey(), n, bi);
				if(bundleWeight.getBundle().size()==0){
					pair.getKey().removeDirectInWeight(bundleWeight);
				}
			}
		}
		
		return sum;
	}


	public static void say(String string) {
		mlog.say("FromOut - "+   string);
	}


	public static void calculatePredictedActivation(HashMap<Integer, INeuron> layer) {
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron ne = pair.getValue();
			ne.calculatePredictedActivation();
		}
	}


	/*public static void increaseDirectInWeights(HashMap<Integer, INeuron> layer) {
		Iterator<Entry<Integer, INeuron>> it = layer.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, INeuron> pair = it.next();
			INeuron n = pair.getValue();
			if(n.getActivation()>0){
				n.increaseDirectInWeights();
			}
		}		
	}*/
}
