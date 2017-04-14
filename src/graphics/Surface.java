package graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import communication.Constants;
import communication.ControllableThread;
import communication.MyLog;


/**class that shows images
 * 
 * @author lana
 *
 */
public class Surface extends JPanel{
	/** log */
	MyLog mlog = new MyLog("Surface", true);
	
	/**
	 * automatically generated
	 */
	private static final long serialVersionUID = -6405382732377682006L;
	
	//controls
	JLabel timeLabel;
	JLabel speedLabel;
	
	/** items to be controlled */
	Vector<ControllableThread> puppets = new Vector<ControllableThread>();
	
	/** frame */
	JFrame frame;
	/**the current letter beng shown*/
	BufferedImage letter;
	/** the partial input to the eye */
	BufferedImage focused;
	/** the partial input to the eye - with adjusted coarseness */
	BufferedImage seen;
	/** the prediction */
	BufferedImage predicted;
	/** the input + stong prediction image*/
	BufferedImage warped;
	/** the action that will be tried next step*/
	String action = "nothing";

	
	/** current center of focus in the image [x,y] (eye tracking) */
	int[] track = {100,100};
	
	/** Size of the eye focus*/
	int eyeFocusSize_h = 25;
	int eyeFocusSize_w = 25;
	/** size of the complete visual field: height */
	int visualField_h = 50;
	/** size of the complete visual field: width */
	int visualField_w = 50;
	
	/** boolean linked to stop/start button*/
	boolean paused = false;
	
	/**
	 * 
	 * @param vh visual field h
	 * @param vw visual field w
	 * @param fh focus h
	 * @param fw focus w
	 */
	public Surface(){
		setVisualFieldSize(Constants.ih, Constants.iw);
		setFocusSize(Constants.ef_h, Constants.ef_h);
		
		letter = new BufferedImage(visualField_w,visualField_h,BufferedImage.TYPE_INT_RGB);
		focused = letter;
		seen = letter;
		
		predicted = new BufferedImage(visualField_w,visualField_h,BufferedImage.TYPE_INT_RGB);
		warped = new BufferedImage(visualField_w,visualField_h,BufferedImage.TYPE_INT_RGB);
		
		buildFrame(this);
	}
	
	/**
	 * add elements to be controlled by the UI
	 * @param t
	 */
	public void addControllable(ControllableThread t) {
		puppets.addElement(t);
	}
	
	
	/**
	 * 
	 * @param l the letter (character), complete input
	 * @param f the partial input to the eye
	 * @param s the partial input to the eye - with adjusted coarseness
	 * @param track current center of focus in the image [x,y] (equivalent to eye tracking)
	 */
	public void setComponents(BufferedImage l, BufferedImage f, BufferedImage s, int[] track){
		letter = l;
		focused = f;
		seen = s;
		this.track = track.clone();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int origin = 10;
		int margin = 10;
		int y = origin+margin;

		
		//complete environmental input
		g.drawString("Original Image", origin, y);
		y = y+margin;
		g.drawImage(letter, origin, y, null);
		
		//focus field outline
		g.setColor(Color.red);
		int th = (eyeFocusSize_h)/2;
		int tw = (eyeFocusSize_w)/2;
		g.drawRect(origin+track[0]-tw, y+track[1]-th, eyeFocusSize_w, eyeFocusSize_h);
		//g.drawRect(origin, y, eyeFocusSize_w, eyeFocusSize_h);
		
		//complete visual field
		th = (visualField_h)/2;
		tw = (visualField_w)/2;
		g.drawRect(origin+track[1]-tw, y+track[0]-th, visualField_w, visualField_h);
		   
		g.setColor(Color.black);
		
		//partial input
		y = y+letter.getHeight()+margin*2;
		g.drawString("Input", origin, y);
		y = y+margin;
		g.drawImage(seen, 10, y, null);  
		
		//input to the eye (partial and blurry)
		y = y+seen.getHeight()+margin*2;
		g.drawString("Before coarse graining", origin, y);
		y = y+margin;
		g.drawImage(focused, 10, y, null);
		
		//chosen action
		g.drawString("Action: "+ action, origin+focused.getWidth()+margin, y);
		
		//prediction
		y = y+letter.getHeight()+margin*2;
		g.drawString("Prediction", origin, y);
		y = y+margin;
		g.drawImage(predicted, 10, y, null);  
		
		//actually seen
		y = y+letter.getHeight()+margin*2;
		g.drawString("Warp", origin, y);
		y = y+margin;
		g.drawImage(warped, 10, y, null);  
    }
	
	/**
	 * The hierarchy is reversed. TODO correct this.
	 * @param s
	 * @return
	 */
	private JFrame buildFrame(Surface s){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(300,700);		
		frame.add(s);
		frame.setTitle("SNET");
		
		//controls
		JFrame ctrlFrame = new JFrame();
		ctrlFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		ctrlFrame.setSize(500,100);
		ctrlFrame.setLayout(new BoxLayout(ctrlFrame.getContentPane(),BoxLayout.X_AXIS));
		ctrlFrame.setTitle("Controls");
		
		//pause/start
		//time label
		timeLabel = new JLabel("Time: 0");
		ctrlFrame.add(timeLabel);
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
	        	 
	        	 for (Iterator<ControllableThread> iterator = puppets.iterator(); iterator.hasNext();) {
					ControllableThread p = iterator.next();
					//mlog.say("a puppet");
					p.setPaused(paused);				
				}
	         }          
	    });
		ctrlFrame.add(pauseButton);
		
		//speed
		//label
		speedLabel = new JLabel("Speed: 1");
		ctrlFrame.add(speedLabel);
		//buttons
		JButton speedDownButton = new JButton("-"); 		
		speedDownButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {	        	 
	        	 for (Iterator<ControllableThread> iterator = puppets.iterator(); iterator.hasNext();) {
					ControllableThread p = iterator.next();
					//just take the last one?
					int speed = p.speedDown();	
					speedLabel.setText("Speed: "+ speed);
				}
	         }          
	    });
		ctrlFrame.add(speedDownButton);
		
		JButton speedUpButton = new JButton("+"); 		
		speedUpButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {	        	 
	        	 for (Iterator<ControllableThread> iterator = puppets.iterator(); iterator.hasNext();) {
					ControllableThread p = iterator.next();
					int speed = p.speedUp();
					speedLabel.setText("Speed: "+ speed);
				}
	         }          
	    });
		ctrlFrame.add(speedUpButton);
		
		//save
		JButton saveButton = new JButton("Save"); 		
		saveButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {	        	 
	        	 for (Iterator<ControllableThread> iterator = puppets.iterator(); iterator.hasNext();) {
					ControllableThread p = iterator.next();
					p.save(saveButton);
				}
	         }          
	    });
		ctrlFrame.add(saveButton);
		
		//refresh graph layout
		JButton refreshButton = new JButton("Redraw"); 		
		refreshButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {	        	 
	        	 for (Iterator<ControllableThread> iterator = puppets.iterator(); iterator.hasNext();) {
					ControllableThread p = iterator.next();
					mlog.say("refresh");
					p.refresh();
				}
	         }          
	    });
		ctrlFrame.add(refreshButton);

		frame.setVisible(true);   
		ctrlFrame.setLocation(310, 10);
		ctrlFrame.setVisible(true); 
		
		//graphics creation
    	int delay = 50; //milliseconds 	
        ActionListener taskPerformer = new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            s.repaint();
          }
        };
        new Timer(delay, taskPerformer).start();
        
		return frame;
	}


	public void setPredicted(BufferedImage prediction) {
		predicted = prediction;
	}


	public void setTime(int step) {
		timeLabel.setText("Time: "+step);	
	}
	
	public void setAction(String action) {
		this.action = action;
	}

	public void setFocusSize(int ih, int iw) {
		eyeFocusSize_h = ih;
		eyeFocusSize_w = iw;
	}

	public void setVisualFieldSize(int ih, int iw) {
		visualField_h = ih;
		visualField_w = iw;
	}
}
