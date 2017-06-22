package graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import communication.Constants;
import communication.MyLog;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import neurons.INeuron;
import neurons.ProbaWeight;
import sensors.Eye;


/**
 * This class manages visualization of the spiking NN.
 * @author lana
 *
 */
public class NetworkGraph {
	MyLog mlog = new MyLog("graphViz", true);
	String name = "SNet";
	
    JFrame frame = new JFrame(name);

	/**Graph<V, E> where V is the type of the vertices and E is the type of the edges*/
	Graph<NeuronVertex, SynapseEdge> g = new DirectedSparseGraph<NeuronVertex, SynapseEdge>();
    /** visualizer*/
    BasicVisualizationServer<NeuronVertex,SynapseEdge> vv;
    /** number of neurons*/
    int n;    
    /** only connections above this weight will be displayed*/
    double minWeight = 0.9;
    /** id, graphical object */
    static HashMap<Integer, NeuronVertex> vertices;
    
    /** whether to draw the frame or not*/
    boolean paused = true;
    
    /** sensors */
    HashMap<Integer, INeuron>[] eye_neurons;
    /**neurons*/
    HashMap<Integer, INeuron> neurons;
    /** currently displayed */
    HashMap<Integer, INeuron> displayed_neurons;
    //use this in a better way
    int grayscale = -1;
    
    /** */
    /*int focus_first;
    int focus_last;
    int outfocus_first;
    int outfocus_last;*/
    /** number of columns in focus */
    int f_ncols;
    /** number of columns outfocus */
    int of_ncols;
    
    /** offset graph at t=0 (dirty)*/
    static int done_centering = 0;
    
    // = eye.getNeuralInterface();
    int[][] n_interface;
    /** maps actual values in world to sensors (square sensory field, can be overlapping) 
	 * [sensor id][topmost sensor, leftmost sensor, size]*/
	int[][] eye_interface;
	
   /**
    * Creates a new instance of NeuronGraph 
    * @param size number of neurons
    * @param weights weights of the network [from][to]
    */
    public NetworkGraph(HashMap<Integer, INeuron> neurons, HashMap<Integer, INeuron>[] eye_neurons, Eye eye){
    	this.neurons = neurons;
    	this.eye_neurons = eye_neurons;
    	displayed_neurons = neurons;
    	n_interface = eye.getNeuralInterface();
    	eye_interface = eye.getEyeInterface();
    	populateGraph(displayed_neurons); 
    }
  
    private void populateGraph(HashMap<Integer, INeuron> neurons){
    	n = neurons.size();
        vertices = new HashMap<>();
        // Add neurons
    	for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
    		INeuron iNeuron = (INeuron) iterator.next();
    		NeuronVertex nv = new NeuronVertex(iNeuron.getId());
    		if(iNeuron.isActivated()){
    			nv.setSpiking(true);
    		}
    		vertices.put(iNeuron.getId(), nv);
    		g.addVertex(nv);
    	}
      
        //add edges
        for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
			INeuron iNeuron = (INeuron) iterator.next();
			//iterate over outweights
			HashMap<INeuron, ProbaWeight> weights = iNeuron.getOutWeights();
			for (Iterator<Entry<INeuron, ProbaWeight>> iterator2 = weights.entrySet().iterator(); iterator2.hasNext();) {
				Entry<INeuron, ProbaWeight> pair = iterator2.next();
				ProbaWeight p = pair.getValue();
				INeuron out = pair.getKey();
				if(p.getProba()>minWeight){
					String label = "w_"+iNeuron.getId()+","+out.getId();
        			SynapseEdge se = new SynapseEdge(label, p.getProba());
        			g.addEdge(se, vertices.get(iNeuron.getId()), vertices.get(out.getId()));
				}
			}
		}        
    }
    
    /**
     * deletes all vertices and edges
     * */
    private void emptyGraph() {
    	for (Iterator<Integer> iterator = vertices.keySet().iterator(); iterator.hasNext();) {
			Integer id = iterator.next();
			NeuronVertex nv = vertices.get(id);
			//delete outdated edges first
			Vector<SynapseEdge> edges = new Vector<SynapseEdge>(g.getOutEdges(nv));
			for (Iterator<SynapseEdge> iterator2 = edges.iterator(); iterator2.hasNext();) {
				SynapseEdge edge = iterator2.next();
				g.removeEdge(edge);
			}
			//delete neuron
			g.removeVertex(nv);
		}
    	//delete from vertices collection
    	vertices.clear();
	}
    
    public void setName(String n){
    	this.name = n;
    }
    
    /**
     * updates displayed neurons
     * @param ne
     */
    public void updateNeurons(){//HashMap<Integer, INeuron> neurons) {
    	
    	if(!paused){
	    	//remove neurons
	    	Vector<NeuronVertex> toBeRemoved = new Vector<NeuronVertex>();
	    	for (Iterator<Integer> iterator = vertices.keySet().iterator(); iterator.hasNext();) {
				Integer id = iterator.next();
				if(!displayed_neurons.containsKey(id)){
					NeuronVertex nv = vertices.get(id);
					//delete outdated edges first
					Vector<SynapseEdge> edges = new Vector<SynapseEdge>(g.getOutEdges(nv));
					for (Iterator<SynapseEdge> iterator2 = edges.iterator(); iterator2.hasNext();) {
						SynapseEdge edge = iterator2.next();
						g.removeEdge(edge);
					}
					//delete neuron
					g.removeVertex(nv);
					toBeRemoved.addElement(nv);
				}
			}
	    	//delete from vertices collection
	    	for (Iterator<NeuronVertex> iterator = toBeRemoved.iterator(); iterator.hasNext();) {
				NeuronVertex neuronVertex = iterator.next();
				vertices.remove(neuronVertex.id);
			}
	    	
	    	// Add neurons
	    	for (Iterator<INeuron> iterator = displayed_neurons.values().iterator(); iterator.hasNext();) {
	    		INeuron iNeuron = iterator.next();
	    		if(!vertices.containsKey(iNeuron.getId())){
	    			NeuronVertex nv = new NeuronVertex(iNeuron.getId());   		
	    			vertices.put(iNeuron.getId(), nv);
	        		g.addVertex(nv);
	        		//mlog.say("added");
	    		}  
	    		NeuronVertex v = vertices.get(iNeuron.getId());
	    		v.setSpiking(false);
	    		if(iNeuron.isActivated()){
	    			v.setSpiking(true);
	    		}  
    			v.setPredicted(false);
	    		if(iNeuron.getPredictedActivation()>0){
	    			v.setPredicted(true);
	    		}
	    	}
	    	
	    	
	    	 //add edges
	        for (Iterator<INeuron> iterator = displayed_neurons.values().iterator(); iterator.hasNext();) {
				INeuron from = iterator.next();
				//iterate over outweights
				HashMap<INeuron, ProbaWeight> weights = from.getOutWeights();
				for (Iterator<Entry<INeuron, ProbaWeight>> iterator2 = weights.entrySet().iterator(); iterator2.hasNext();) {
					Entry<INeuron, ProbaWeight> pair = iterator2.next();
					ProbaWeight p = pair.getValue();
					INeuron out = pair.getKey();
					//weight is high and edge does not exist
					if(p.getProba()>minWeight){				
						if((g.findEdge(vertices.get(from.getId()), vertices.get(out.getId()))==null)){ 
							String label = "w_"+from.getId()+","+out.getId();
		        			SynapseEdge se = new SynapseEdge(label, p.getProba());
		        			g.addEdge(se, vertices.get(from.getId()), vertices.get(out.getId()));
						}
					}else{
						//weight is low but edge does exist
						SynapseEdge s;
						if((s = g.findEdge(vertices.get(from.getId()), vertices.get(out.getId())))!=null){ 
							//mlog.say("found");
							g.removeEdge(s);
						}
					}
						
				}
			}  
	    	
	    	//repaint out of main thread
	    	Runnable code = new Runnable() {
	        	public void run() {
	        		vv.repaint();
	        	}
	        };
	
	        (new Thread(code)).start();
    	}
    }
        
    
    /**
     * 
     * @param square true if displaying sensors
     */
    public void show() {
    	squareLayout(false); 
        //frame.getContentPane().add(vv, BorderLayout.CENTER); 

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));
		
		//pause button
		JButton pauseButton = new JButton("Start"); 		
		pauseButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	        	 if(!paused){
	        		 paused = true;
	        		 pauseButton.setText("Start");
	        	 }else{
	        		 paused = false;
	        		 pauseButton.setText("Pause");
	        	 }	
	         }          
	    });
		frame.add(pauseButton);
		
		//dropdown list of sensor layers / hidden modules
		String[] choices = { "Neurons","White", "Gray 1","Gray 2","Gray 3","Gray 4", "Black"};
	
	    final JComboBox<String> cb = new JComboBox<String>(choices);
	    ActionListener cbActionListener = new ActionListener() {//add actionlistner to listen for change
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) cb.getSelectedItem();//get the selected item
                grayscale = -1;
                displayed_neurons = neurons;
            	emptyGraph();
                frame.getContentPane().remove(vv); 

                switch (s) {
                    case "Neurons":
                    	updateGrayscale(-1);
                        break;
                    case "White":
                    	updateGrayscale(0);
                        break;
                    case "Gray 1":
                    	updateGrayscale(1);
                        break;
                    case "Gray 2":
                    	updateGrayscale(2);
                        break;
                    case "Gray 3":
                    	updateGrayscale(3);
                        break;
                    case "Gray 4":
                    	updateGrayscale(4);
                        break;
                    case "Black":
                    	updateGrayscale(Constants.gray_scales-1);
                        break;
                } 
                
                frame.getContentPane().add(vv, BorderLayout.CENTER); 
                vv.repaint();
                frame.revalidate();
            }
        };
        cb.addActionListener(cbActionListener);
	    cb.setVisible(true);
	    frame.add(cb);
		
        frame.setLocation(310, 150);
        frame.getContentPane().add(vv, BorderLayout.CENTER); 
        frame.pack();      
        frame.setVisible(true);       
    }
    
    public void setHiddenLayer(HashMap<Integer, INeuron> d_neurons) {
    	neurons = d_neurons;
		if(grayscale<0){
			updateNeurons();
		}
	}
    
    private void updateGrayscale(int g) {
		grayscale = g;
		if(g<0){
			displayed_neurons = neurons;
		}else{
			displayed_neurons = eye_neurons[grayscale];
		}
		populateGraph(displayed_neurons);
		if(g<0){
	    	squareLayout(false);
		} else {
			squareLayout(true);
		}
	}
    
    private void squareLayout(boolean square){
    	int w = 600, h = 600;
    	Layout<NeuronVertex, SynapseEdge>  layout;
		
		if(square){
			layout = new StaticLayout<NeuronVertex, SynapseEdge>(g);
			layout.setSize(new Dimension(w,h)); // sets the initial size of the layout space
			
			/*Iterator<Entry<Integer, NetworkGraph.NeuronVertex>> it = vertices.entrySet().iterator();
			int x=0, y=0;
			while(it.hasNext()){
				Map.Entry<Integer, NetworkGraph.NeuronVertex> pair = it.next();
				NetworkGraph.NeuronVertex v = pair.getValue();
				layout.setLocation(v, new Point2D.Float((float)x,(float)y));
		    	layout.lock(v, true);
		    	x+=10;
		    	y+=10;
			}*/
			//size
			int factor = 600/Constants.vf_w;
		 	
		 	int nt = n_interface[0].length;
		 	for(int k = 0; k<nt; k++){
			 	int sensor_i = eye_interface[k][0];
				int sensor_j = eye_interface[k][1];
				int size = eye_interface[k][2];//size of the zone for this sensor
				int nid = n_interface[grayscale][k];
				//displayed_neurons.get(nid);
				NeuronVertex v = vertices.get(nid);
				float x = sensor_i+size/2;
				float y = sensor_j+size/2;
				layout.setLocation(v, new Point2D.Float(x*factor,y*factor));
		    	layout.lock(v, true);
		 	}//*/
		} else {
			layout = new ISOMLayout<NeuronVertex, SynapseEdge>(g);
			layout.setSize(new Dimension(w,h)); // sets the initial size of the layout space
		}
		
		 // The BasicVisualizationServer<V,E> is parameterized by the vertex and edge types
		 vv = new BasicVisualizationServer<NeuronVertex,SynapseEdge>(layout);
		 vv.setPreferredSize(new Dimension(w+50,h+50)); //Sets the viewing area size
		 vv.getRenderer().setVertexRenderer(new MyRenderer());
		 vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		 vv.getRenderContext().setVertexLabelTransformer(new Transformer<NeuronVertex, String>() {
		     public String transform(NeuronVertex nv) {
		         return (nv.toString());
		     }
		 });
		 
    }
    
    /**
     * Custom node class to display neurons
     * @author lana
     *
     */
    class NeuronVertex {
    	 int id; 
    	 private boolean isSpiking = false;
    	 private boolean isPredicted = false;
    	 
    	 public NeuronVertex(int id) {
    		 this.id = id;
    	 }
    	 public void setSpiking(boolean b){
    		 isSpiking = b;
    	 }
    	 
    	 public void setPredicted(boolean b) {
			isPredicted = b;
    	 }
    	    	 
    	 public String toString() { 
    		 return ""+id; 
    	 }
    }
    
    /**
     * Custom edge class
     * @author lana 
     */

    class SynapseEdge{
    	double weight = 0;
    	String label;
    	
    	public SynapseEdge(String label, double weight){
    		this.label = label;
    		this.weight = weight;
    	}
    	
    	public String toString() { 
    		return label; 
   	 	}
    }
    
    /**
     * Custom vertex visualization.
     * @author lana
     *
     */
    static class MyRenderer implements Vertex<NeuronVertex, SynapseEdge> {    	
    	Color blue = new Color(102, 153, 204);
    	
    	int ofx = 0;
    	int ofy = 0;
    	
		public void paintVertex(RenderContext<NeuronVertex, SynapseEdge> rc, Layout<NeuronVertex, SynapseEdge> layout, NeuronVertex vertex) {

			GraphicsDecorator graphicsContext = rc.getGraphicsContext();
			Point2D center = layout.transform(vertex);
			layout.setLocation(vertex, new Point2D.Double(center.getX()+ofx, center.getY()+ofy));
			Shape shape = null;
			Color color = null;
			Color edgeColor = Color.black;
			  
			shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
			NeuronVertex nv = vertices.get(vertex.id);
			if(nv.isSpiking) {	    
				color = blue;	         
			} else{
				color = Color.lightGray;  	        	  
			}
			if(nv.isPredicted){
				edgeColor = Color.red;
			}
			graphicsContext.setPaint(edgeColor);
			graphicsContext.draw(shape);
			graphicsContext.setPaint(color);
			graphicsContext.fill(shape);
		}
    }
    

    /** 
     * refreshes the graph as to get better alignment of the vertices
     */
	public void redraw() {
		vv.repaint();
	}
	
	public void rebuildGraph(){
		emptyGraph();
		populateGraph(displayed_neurons);
		vv.repaint();
	}
}
