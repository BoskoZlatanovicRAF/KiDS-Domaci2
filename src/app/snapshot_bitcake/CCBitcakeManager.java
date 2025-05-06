package app.snapshot_bitcake;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.snapshot.CCAckMessage;
import servent.message.snapshot.CCSnapshotRequestMessage;
import servent.message.util.MessageUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CCBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    private final AtomicBoolean snapshotBlocked = new AtomicBoolean(false);
    private final AtomicInteger snapshotAmount = new AtomicInteger(0);
    private final Queue<Integer> pendingTransactions = new ConcurrentLinkedQueue<>();

    private volatile boolean processedRequest = false;
    private volatile int initiatorId = -1;

    public CCBitcakeManager() {
        AppConfig.timestampedStandardPrint("Starting with " + currentAmount.get() + " bitcakes.");
    }

    @Override
    public void takeSomeBitcakes(int amount) {
        // Always update the actual balance
        currentAmount.getAndAdd(-amount);

        // If snapshot is blocked, record this transaction for snapshot calculation
        if (snapshotBlocked.get()) {
            pendingTransactions.add(-amount);
        }
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
        if (snapshotBlocked.get()) {
            pendingTransactions.add(amount);
        }
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void blockSnapshot() {
        snapshotAmount.set(currentAmount.get());
        snapshotBlocked.set(true);
    }

    public void unblockSnapshot() {
        snapshotBlocked.set(false);
        snapshotAmount.set(0);
    }

    public int getSnapshotAmount() {
        if (snapshotBlocked.get()) {
            return snapshotAmount.get();
        }
        return getCurrentBitcakeAmount();
    }

    public void handleSnapshotRequest(Message requestMessage, SnapshotCollector collector) {
        int reqInitiatorId = requestMessage.getOriginalSenderInfo().getId();
        int senderId = requestMessage.getOriginalSenderInfo().getId();

        if (!snapshotBlocked.get()) {
            AppConfig.timestampedStandardPrint("Processing snapshot request from initiator " + reqInitiatorId);

            blockSnapshot();
            int localAmount = getSnapshotAmount();
            AppConfig.timestampedStandardPrint("CC snapshot - local state recorded: " + localAmount);

            CCSnapshotResult result = new CCSnapshotResult(AppConfig.myServentInfo.getId(), localAmount);

            if (reqInitiatorId == AppConfig.myServentInfo.getId()) {
                ((SnapshotCollectorWorker) collector).addCCSnapshotInfo(AppConfig.myServentInfo.getId(), result);
            } else {
                CCAckMessage ackMessage = new CCAckMessage(AppConfig.myServentInfo, AppConfig.getInfoById(reqInitiatorId), String.valueOf(localAmount));
                MessageUtil.sendMessage(ackMessage);
            }

            for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
                if (neighborId == senderId) {
                    continue; // Don't send back to sender
                }

                ServentInfo neighborInfo = AppConfig.getInfoById(neighborId);
                CCSnapshotRequestMessage forwardRequest = new CCSnapshotRequestMessage(requestMessage.getOriginalSenderInfo(), neighborInfo);
                AppConfig.timestampedStandardPrint("Forwarding CC_SNAPSHOT_REQUEST to " + neighborId);
                MessageUtil.sendMessage(forwardRequest);
            }
        }
    }
}

