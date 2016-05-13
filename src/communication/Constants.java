package communication;

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
	
	/** sensitivity of the image sensor */
	public static final int gray_scales = 4;
}
