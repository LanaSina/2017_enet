package graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import communication.ControllableThread;
import communication.MyLog;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import neurons.INeuron;
import neurons.ProbaWeight;



/**
 * This class manages visualization of the spiking NN.
 * @author lana
 *
 */
public class NetworkGraph {
	MyLog mlog = new MyLog("graphViz", true);
	String name = "SNet";
	
	/**Graph<V, E> where V is the type of the vertices and E is the type of the edges*/
	Graph<NeuronVertex, SynapseEdge> g = new DirectedSparseGraph<NeuronVertex, SynapseEdge>();
    /** visualizer*/
    BasicVisualizationServer<NeuronVertex,SynapseEdge> vv;
    /** number of neurons*/
    int n;
    /** only connections above this weight will be displayed*/
    double minWeight = 0.9;
    //static NeuronVertex[] vertices;  
    /** id, graphical object */
    static HashMap<Integer, NeuronVertex> vertices;
    
    /** whether to draw the frame or not*/
    boolean paused = false;

   /**
    * Creates a new instance of NeuronGraph 
    * @param size number of neurons
    * @param weights weights of the network [from][to]
    */
    public NetworkGraph(HashMap<Integer, INeuron> neurons){
    	populateGraph(neurons);      
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
     * @param ne
     */
    public void updateNeurons(HashMap<Integer, INeuron> neurons) {
    	
    	if(!paused){
	    	//remove neurons
	    	Vector<NeuronVertex> toBeRemoved = new Vector<NeuronVertex>();
	    	for (Iterator<Integer> iterator = vertices.keySet().iterator(); iterator.hasNext();) {
				Integer id = iterator.next();
				if(!neurons.containsKey(id)){
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
	    	for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
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
	        for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
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
        
    
    public void show() {
        // The Layout<V, E> is parameterized by the vertex and edge types       
    	//Layout<NeuronVertex, String> layout = new FRLayout(g);//CircleLayout(g);
    	Layout<NeuronVertex, SynapseEdge>  layout = new ISOMLayout<NeuronVertex, SynapseEdge>(g);

        layout.setSize(new Dimension(700,700)); // sets the initial size of the layout space
        // The BasicVisualizationServer<V,E> is parameterized by the vertex and edge types
        vv = new BasicVisualizationServer<NeuronVertex,SynapseEdge>(layout);
        vv.setPreferredSize(new Dimension(800,800)); //Sets the viewing area size
        vv.getRenderer().setVertexRenderer(new MyRenderer());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<NeuronVertex, String>() {
            public String transform(NeuronVertex nv) {
                return (nv.toString());
            }
        });//*/
        
        
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));
		
		//pause button
		JButton pauseButton = new JButton("Pause"); 		
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
		
		
		
		
        frame.setLocation(310, 150);
        frame.getContentPane().add(vv, BorderLayout.CENTER); 
        frame.pack();      
        frame.setVisible(true);       
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
	          Shape shape = null;
	          Color color = null;
	          Color edgeColor = Color.black;
	          
	          shape = new Ellipse2D.Double(center.getX()-10+ofx, center.getY()-10+ofy, 20, 20);
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
	public void redraw(HashMap<Integer, INeuron> neurons) {
		emptyGraph();
		populateGraph(neurons);
		vv.repaint();
	}
    
    /**
     * Custom label visualization. TODO
     * @author lana
     *
     */
   /* static class MyLabelRenderer implements Vertex<NeuronVertex, SynapseEdge> {    	
    	Color blue = new Color(102, 153, 204);
    	int ofx = 0;
    	int ofy = 0;

		public void paintVertex(RenderContext<NeuronVertex, SynapseEdge> rc, Layout<NeuronVertex, SynapseEdge> layout, NeuronVertex vertex) {
			GraphicsDecorator graphicsContext = rc.getGraphicsContext();
	          Point2D center = layout.transform(vertex);
	          Shape shape = null;
	          Color color = null;
	          
	          shape = new Ellipse2D.Double(center.getX()-10+ofx, center.getY()-10+ofy, 20, 20);
	          NeuronVertex nv = vertices.get(vertex.id);
	          if(nv.isSpiking) {	    
	        	  color = blue;	         
	          } else{
	        	  color = Color.lightGray;  	        	  
	          }
	          graphicsContext.setPaint(Color.black);
	          graphicsContext.draw(shape);
	          graphicsContext.setPaint(color);
	          graphicsContext.fill(shape);
		}
     }*/
}
