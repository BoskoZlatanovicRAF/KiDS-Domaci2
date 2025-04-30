package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.snapshot.CCResumeMessage;
import servent.message.snapshot.CCSnapshotRequestMessage;
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


	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		
		switch(snapshotType) {
			case COORDINATED_CHECKPOINTING:
				bitcakeManager = new CCBitcakeManager();
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
				case COORDINATED_CHECKPOINTING:
					AppConfig.timestampedStandardPrint("Initiating coordinated snapshot");

					// Start the snapshot locally
					if (bitcakeManager instanceof CCBitcakeManager ccManager) {
						// Create a self-message to initiate the snapshot
						Message selfRequest = new CCSnapshotRequestMessage(
								AppConfig.myServentInfo,
								AppConfig.myServentInfo
						);
						ccManager.handleSnapshotRequest(selfRequest, this);
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
					case COORDINATED_CHECKPOINTING:
						AppConfig.timestampedStandardPrint("Checking for completion: " +
								collectedCCValues.size() + "/" + AppConfig.getServentCount());

						if (collectedCCValues.size() == AppConfig.getServentCount()) {
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
					sum = 0;
					for (Entry<String, Integer> itemAmount : collectedNaiveValues.entrySet()) {
						sum += itemAmount.getValue();
						AppConfig.timestampedStandardPrint(
								"Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
					}

					AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

					collectedNaiveValues.clear(); //reset for next invocation
					break;

				case COORDINATED_CHECKPOINTING:
					// Check if we have collected all responses
					if (collectedCCValues.size() == AppConfig.getServentCount()) {
						// Print results
						sum = 0;
						for (Entry<Integer, CCSnapshotResult> serventResult : collectedCCValues.entrySet()) {
							sum += serventResult.getValue().getRecordedAmount();
							AppConfig.timestampedStandardPrint(
									"Node " + serventResult.getKey() + " has " +
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
