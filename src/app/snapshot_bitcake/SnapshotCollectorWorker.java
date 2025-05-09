package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.ab_acharya_badrinath.ABBitcakeManager;
import app.snapshot_bitcake.ab_acharya_badrinath.ABSnapshotResult;
import app.snapshot_bitcake.av_alagar_venkatesan.AVBitcakeManager;
import app.snapshot_bitcake.av_alagar_venkatesan.AVSnapshotResult;
import app.snapshot_bitcake.cc_coordinated_checkpointing.CCBitcakeManager;
import app.snapshot_bitcake.cc_coordinated_checkpointing.CCSnapshotResult;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABTokenMessage;
import servent.message.snapshot.av.AVTokenMessage;
import servent.message.snapshot.cc.CCResumeMessage;
import servent.message.snapshot.cc.CCSnapshotRequestMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 *
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;

	private AtomicBoolean collecting = new AtomicBoolean(false);

	private Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();

	private SnapshotType snapshotType = SnapshotType.NAIVE;

	private BitcakeManager bitcakeManager;

	// CC algorithm
	private Map<Integer, CCSnapshotResult> collectedCCValues = new ConcurrentHashMap<>();

	// AB algorithm
	private Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<>();
	private Map<Integer, int[]> collectedABSent = new ConcurrentHashMap<>();
	private Map<Integer, int[]> collectedABRecd = new ConcurrentHashMap<>();

	public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult, int[] sent, int[] recd) {
		collectedABValues.put(id, abSnapshotResult);
		collectedABSent.put(id, sent);
		collectedABRecd.put(id, recd);
	}


	// AV algorithm
	private Map<Integer, AVSnapshotResult> collectedAVValues = new ConcurrentHashMap<>();

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;

		switch(snapshotType) {
			case COORDINATED_CHECKPOINTING:
				bitcakeManager = new CCBitcakeManager();
				break;
			case ACHARYA_BADRINATH:
				bitcakeManager = new ABBitcakeManager();
				break;
			case ALAGAR_VENKATESAN:
				bitcakeManager = new AVBitcakeManager();
				break;
			case NONE:
				AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
				System.exit(0);
		}
	}

	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	@Override
	public void run() {
		while(working) {

			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (working == false) {
					return;
				}
			}

			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */

			//1 send asks
			switch (snapshotType) {
				case NAIVE:
					// Existing naive implementation
					break;

				case COORDINATED_CHECKPOINTING:
					AppConfig.timestampedStandardPrint("Initiating coordinated snapshot");

					// Start the snapshot locally
					if (bitcakeManager instanceof CCBitcakeManager ccManager) {
						// Create a self-message to initiate the snapshot
						Message selfRequest = new CCSnapshotRequestMessage(AppConfig.myServentInfo, AppConfig.myServentInfo);
						ccManager.handleSnapshotRequest(selfRequest, this);
					}
					break;

				case ACHARYA_BADRINATH:
					AppConfig.timestampedStandardPrint("Initiating Acharya-Badrinath snapshot");

					//01. The initiator process broadcasts a token to every process, including itself.
					for (int i = 0; i < AppConfig.getServentCount(); i++) {
						ServentInfo serventInfo = AppConfig.getInfoById(i);
						Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

						Message tokenMessage = new ABTokenMessage(AppConfig.myServentInfo, serventInfo,null, vectorClock);
						MessageUtil.sendMessage(tokenMessage);
					}
					break;
				case ALAGAR_VENKATESAN:
					AppConfig.timestampedStandardPrint("Initiating Alagar-Venkatesan snapshot");

					for (int i = 0; i < AppConfig.getServentCount(); i++) {
						ServentInfo serventInfo = AppConfig.getInfoById(i);
						Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

						Message tokenMessage = new AVTokenMessage(MessageType.AV_TOKEN, AppConfig.myServentInfo, serventInfo, vectorClock);
						MessageUtil.sendMessage(tokenMessage);
					}
					break;


				case NONE:
					//Shouldn't be able to come here. See constructor.
					break;
			}

			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
					case NAIVE:
						if (collectedNaiveValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case COORDINATED_CHECKPOINTING:
						AppConfig.timestampedStandardPrint("Checking for completion: " +
								collectedCCValues.size() + "/" + AppConfig.getServentCount());

						if (collectedCCValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case ACHARYA_BADRINATH:
						AppConfig.timestampedStandardPrint("Checking for completion (AB): " +
								collectedABValues.size() + "/" + AppConfig.getServentCount());

						//05. The algorithm completes once the initiator has received states from all processes.
						if (collectedABValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;
					case ALAGAR_VENKATESAN:
						AppConfig.timestampedStandardPrint("Waiting for AV_DONEs: " +
								collectedAVValues.size() + "/" + AppConfig.getServentCount());

						if (collectedAVValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case NONE:
						//Shouldn't be able to come here. See constructor.
						break;
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (working == false) {
					return;
				}
			}

			//print
			int sum;
			switch (snapshotType) {
				case NAIVE:
					// Existing naive implementation
					break;

				case COORDINATED_CHECKPOINTING:
					// Check if we have collected all responses
					if (collectedCCValues.size() == AppConfig.getServentCount()) {
						// Print results
						sum = 0;
						for (Entry<Integer, CCSnapshotResult> serventResult : collectedCCValues.entrySet()) {
							sum += serventResult.getValue().getRecordedAmount();
							AppConfig.timestampedStandardPrint(
									"Servent " + serventResult.getKey() + " has " +
											serventResult.getValue().getRecordedAmount() + " bitcakes");
						}

						AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

						// Send RESUME messages to all servents to unblock transactions
						AppConfig.timestampedStandardPrint("Sending RESUME messages to all servents");
						for (int i = 0; i < AppConfig.getServentCount(); i++) {
							if (i == AppConfig.myServentInfo.getId()) {
								// Unblock ourselves
								if (bitcakeManager instanceof CCBitcakeManager) {
									((CCBitcakeManager) bitcakeManager).unblockSnapshot();
								}
								continue;
							}

							ServentInfo serventInfo = AppConfig.getInfoById(i);
							CCResumeMessage resumeMessage = new CCResumeMessage(AppConfig.myServentInfo, serventInfo);
							MessageUtil.sendMessage(resumeMessage);
						}

						// Reset for next snapshot
						collectedCCValues.clear();
						collecting.set(false);
					}
					break;

				case ACHARYA_BADRINATH:
					// Check if we have collected all responses
					if (collectedABValues.size() == AppConfig.getServentCount()) {
						// Print results
						sum = 0;
						for (Entry<Integer, ABSnapshotResult> serventResult : collectedABValues.entrySet()) {
							sum += serventResult.getValue().getRecordedAmount();
							AppConfig.timestampedStandardPrint(
									"Servent " + serventResult.getKey() + " has " +
											serventResult.getValue().getRecordedAmount() + " bitcakes");
						}

						// Calculate and print channel states
						int channelSum = 0;
						for (int i = 0; i < AppConfig.getServentCount(); i++) {
							for (int j = 0; j < AppConfig.getServentCount(); j++) {
								if (i != j) {
									int[] sent = collectedABSent.get(i);
									int[] recd = collectedABRecd.get(j);

									if (sent != null && recd != null) {
										int channelState = sent[j] - recd[i];
										if (channelState > 0) {
											channelSum += channelState;
											AppConfig.timestampedStandardPrint(
													"Channel from " + i + " to " + j + " has " + channelState + " bitcakes");
										}
									}
								}
							}
						}

						AppConfig.timestampedStandardPrint("System bitcake count: " + (sum + channelSum));

						// Reset for next snapshot
						collectedABValues.clear();
						collectedABSent.clear();
						collectedABRecd.clear();
						collecting.set(false);
					}
					break;
				case ALAGAR_VENKATESAN:
					if (collectedAVValues.size() == AppConfig.getServentCount()) {
						sum = 0;
						for (Entry<Integer, AVSnapshotResult> serventResult : collectedAVValues.entrySet()) {
							sum += serventResult.getValue().getRecordedAmount();
							AppConfig.timestampedStandardPrint(
									"Servent " + serventResult.getKey() + " has " +
											serventResult.getValue().getRecordedAmount() + " bitcakes");
						}

						AppConfig.timestampedStandardPrint("System bitcake count (AV): " + sum);

						collectedAVValues.clear();
						collecting.set(false);
					}
					break;
				case NONE:
					//Shouldn't be able to come here. See constructor.
					break;
			}
			collecting.set(false);
		}
	}

	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
		collectedNaiveValues.put(snapshotSubject, amount);
	}

	@Override
	public void addABSnapshotInfo(String snapshotSubject, int amount, List<Message> sendTransactions, List<Message> receivedTransactions) {
		int id = Integer.parseInt(snapshotSubject.replace("node", ""));

		// Create a snapshot result object
		ABSnapshotResult result = new ABSnapshotResult(id, amount);

		// Create SENT and RECD arrays
		int serventCount = AppConfig.getServentCount();
		int[] sent = new int[serventCount];
		int[] recd = new int[serventCount];

		// Fill arrays from transaction lists
		for (Message msg : sendTransactions) {
			if (msg.getMessageType() == servent.message.MessageType.TRANSACTION) {
				int receiverId = msg.getReceiverInfo().getId();
				sent[receiverId]++;
			}
		}

		for (Message msg : receivedTransactions) {
			if (msg.getMessageType() == servent.message.MessageType.TRANSACTION) {
				int senderId = msg.getOriginalSenderInfo().getId();
				recd[senderId]++;
			}
		}

		// Store the information
		addABSnapshotInfo(id, result, sent, recd);
	}

	@Override
	public void addAVSnapshotInfo(int id, int amount) {
		AVSnapshotResult result = new AVSnapshotResult(id, amount);
		collectedAVValues.put(id, result);
	}

	@Override
	public void addCCSnapshotInfo(int id, CCSnapshotResult ccSnapshotResult) {
		collectedCCValues.put(id, ccSnapshotResult);
	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);

		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	@Override
	public void stop() {
		working = false;
	}
}