package models;
import communication.MyLog;
import graphics.Surface;

/**
 * Rebuilding the whole project in a cleaner way.
 * @author lana
 *
 */
public class CharCNet {
	/** log */
	MyLog mlog = new MyLog("CarCNet", true);
	/** graphics*/
	Surface panel;
	
	public CharCNet(){
		//graphics creation
    	panel = new Surface();
    	
		//main thread
    	new Thread(new ExperimentThread(this)).start();		
	}

	
	//main thread
	private class ExperimentThread implements Runnable {
		/** frequency at which to snap the network */
		int step = 0;
		/** log */
		MyLog mlog = new MyLog("FocusNet Thread", true);
		/** network */
		CharCNet net;
		
		public ExperimentThread(CharCNet net){
			this.net = net;
		}
		
	    public void run() {
	
	    	while(true){
	    		long before = 0;
	    		if(step%50==0){
	    			//calculate runtime
	    			before = System.currentTimeMillis();
	    		}
	    		
	    		//net.buildInputs();
	    		//net.updateCNet();
		
	    		if(step%50==0){
	    			long runtime = System.currentTimeMillis()-before;
	    			//calculate snap time
	    			before = System.currentTimeMillis();
	    			//net.cSnap(); do this when there is a clean consciousness
	    			long snaptime = System.currentTimeMillis()-before;;
	    			mlog.say("runtime "+runtime + " snaptime "+ snaptime);
	    		}
	    		
	    		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    		
	    		step++;
	    	}
	    }
	
	}
		
}
