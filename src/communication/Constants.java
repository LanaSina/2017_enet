package communication;

import java.util.Random;

/**
 * Global variables
 * @author lana
 *
 */
public class Constants {
	
	/** controls output of all loggers. Set true for verbose mode, false for dry mode.*/
	public static final boolean shouldLog = true;


	/** age at which probabilistic connections stop learning */
	public static final int weight_max_age = 100;
	/** confidence threshold for predictions */
	public static final double confidence_threshold = 0.9;//90%
	
	/** sensitivity of the image sensor */
	public static final int gray_scales = 4;


	//weight types
	/** this connection weight will always be max_weight (=1)*/
	public static final int fixedConnection = 1;
	/** this connection weight will change with time*/
	public static final int defaultConnection = 2;
	
	
	
	//functions
	
	/**
	 * from http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
	 * and http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range
	 * Returns a pseudo-random number between min and max, inclusive.
	 * Uniform distribution.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimum value
	 * @param max Maximum value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static double uniformDouble(double min, double max) {
	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    return randomNum;
	}
	
}
