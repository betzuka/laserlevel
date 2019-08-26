package betzuka.tools.laserlevel;


public class SampleAverager implements FrameListener {

	public interface Tracker {
		public void onUpdate(int samplesTaken);
		public void onComplete(double avg);
	}
	
	private double sum = 0;
	private int samplesToTake;
	private int samplesTaken = 0;
	private Tracker tracker;
	
	public SampleAverager(int samplesToTake, Tracker tracker) {
		this.samplesToTake = samplesToTake;
		this.tracker = tracker;
	}
	
	public void add(double v) {
		if (samplesTaken < samplesToTake) {
			sum += v;
			samplesTaken++;
			tracker.onUpdate(samplesTaken);
			if (isDone()) {
				tracker.onComplete(getAverage());
			}
		}
	}
	
	public boolean isDone() {
		return samplesTaken==samplesToTake;
	}
	
	public double getAverage() { 
		if (isDone()) {
			return sum/(double)samplesToTake;
		}
		return Double.NaN;
	}

	@Override
	public boolean onFrame(AnalyzedFrame frame) {
		if (frame.hasFit()) {
			add(frame.getMaxima());
		}
		return isDone();
	}
	
}
