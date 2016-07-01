package communication;

/**
 * Interface for threads to be controlled by the Surface UI
 * @author lana
 *
 */
public interface ControllableThread {
	public void setPaused(boolean paused);
	
	public int speedUp();
	
	public int speedDown();
}
