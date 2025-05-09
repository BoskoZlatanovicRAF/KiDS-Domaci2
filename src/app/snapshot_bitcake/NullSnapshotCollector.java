package app.snapshot_bitcake;

import app.snapshot_bitcake.cc_coordinated_checkpointing.CCSnapshotResult;
import servent.message.Message;

import java.util.List;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {}

	@Override
	public void addABSnapshotInfo(String snapshotSubject, int amount, List<Message> sendTransactions, List<Message> receivedTransactions) {

	}

	@Override
	public void addAVSnapshotInfo(int id, int amount) {

	}

	@Override
	public void addCCSnapshotInfo(int id, CCSnapshotResult ccSnapshotResult) {}


	@Override
	public void startCollecting() {}

}
