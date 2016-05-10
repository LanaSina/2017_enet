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
		//surface
		
		//main thread
	}

	
	//main thread
	private class ExperimentThread implements Runnable {
		int step = 0;
		/** log */
		MyLog mlog = new MyLog("FocusNet Thread", true);
		
	    public void run() {
	    	CharCNet net = new CharCNet();
	
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
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    		
	    		step++;
	    	}
	    }
	
	}
		
}
