package soot.jimple.infoflow.data.pathBuilders;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Path builder that forwards all its requests to another path builder in
 * batches. This builder waits for each batch to complete before submitting
 * another batch. Use this path builder to reduce the memory consumption of the
 * path building process by keeping less paths in memory at the same time.
 * 
 * @author Steven Arzt
 *
 */
public class BatchPathBuilder extends AbstractAbstractionPathBuilder {

	private final IAbstractionPathBuilder innerBuilder;
	private int batchSize = 5;
	private ISolverTerminationReason terminationReason = null;

	public BatchPathBuilder(InfoflowManager manager, IAbstractionPathBuilder innerBuilder) {
		super(manager);
		this.innerBuilder = innerBuilder;
	}

	@Override
	public void computeTaintPaths(Set<AbstractionAtSink> res) {
		Set<AbstractionAtSink> batch = new HashSet<>();
		Iterator<AbstractionAtSink> resIt = res.iterator();
		int batchId = 1;
		while (resIt.hasNext()) {
			// Build the next batch
			while (batch.size() < this.batchSize && resIt.hasNext())
				batch.add(resIt.next());
			logger.info(
					String.format("Running path reconstruction batch %d with %d elements", batchId++, batch.size()));

			// Run the next batch
			innerBuilder.reset();
			innerBuilder.computeTaintPaths(batch);

			// Save the termination reason
			if (this.terminationReason == null)
				this.terminationReason = innerBuilder.getTerminationReason();
			else
				this.terminationReason = this.terminationReason.combine(innerBuilder.getTerminationReason());

			// Wait for the batch to complete
			if (innerBuilder instanceof ConcurrentAbstractionPathBuilder) {
				ConcurrentAbstractionPathBuilder concurrentBuilder = (ConcurrentAbstractionPathBuilder) innerBuilder;
				final InterruptableExecutor resultExecutor = concurrentBuilder.getExecutor();
				try {
					resultExecutor.awaitCompletion();
				} catch (InterruptedException e) {
					logger.error("Could not wait for executor termination", e);
				}
				resultExecutor.reset();
			}

			// Prepare for the next batch
			batch.clear();
		}
	}

	@Override
	public InfoflowResults getResults() {
		return innerBuilder.getResults();
	}

	@Override
	public void runIncrementalPathCompuation() {
		innerBuilder.runIncrementalPathCompuation();
	}

	@Override
	public IAbstractionPathBuilder getInnerBuilder() {
		return this.innerBuilder;
	}

	@Override
	public void setPropagationResults(Set<AbstractionAtSink> res) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forceTerminate(ISolverTerminationReason reason) {
		innerBuilder.forceTerminate(reason);
	}

	@Override
	public boolean isTerminated() {
		return innerBuilder.isTerminated();
	}

	@Override
	public boolean isKilled() {
		return innerBuilder.isKilled();
	}

	@Override
	public ISolverTerminationReason getTerminationReason() {
		return terminationReason;
	}

	@Override
	public void reset() {
		innerBuilder.reset();
	}

	@Override
	public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
		innerBuilder.addStatusListener(listener);
	}

	/**
	 * Sets the number of paths that shall be part of one batch, i.e., that shall be
	 * forwarded to the inner path builder at the same time
	 * 
	 * @param batchSize
	 *            The number of paths in one batch
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
