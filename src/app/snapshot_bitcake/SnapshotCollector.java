package app.snapshot_bitcake;

import app.Cancellable;
import app.snapshot_bitcake.cc_coordinated_checkpointing.CCSnapshotResult;
import servent.message.Message;

import java.util.List;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();

	void addNaiveSnapshotInfo(String snapshotSubject, int amount);

	void addABSnapshotInfo(String snapshotSubject, int amount, List<Message> sendTransactions, List<Message> receivedTransactions);

	void addAVSnapshotInfo(int id, int amount);

	void addCCSnapshotInfo(int id, CCSnapshotResult ccSnapshotResult);

	void startCollecting();

}