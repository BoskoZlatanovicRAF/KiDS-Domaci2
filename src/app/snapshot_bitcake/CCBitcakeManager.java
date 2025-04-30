package app.snapshot_bitcake;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.snapshot.CCAckMessage;
import servent.message.snapshot.CCSnapshotRequestMessage;
import servent.message.util.MessageUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CCBitcakeManager implements BitcakeManager{
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    private final AtomicBoolean snapshotBlocked = new AtomicBoolean(false);

    // Add a flag to track if we've processed a request in this snapshot round
    private volatile boolean processedRequest = false;
    private volatile int initiatorId = -1;

    @Override
    public void takeSomeBitcakes(int amount) {
        if (snapshotBlocked.get()) return;
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        if (snapshotBlocked.get()) return;
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void blockSnapshot() {
        snapshotBlocked.set(true);
    }

    public void unblockSnapshot() {
        snapshotBlocked.set(false);
        processedRequest = false;
        initiatorId = -1;
    }

    /**
     * Handle a snapshot request - record state and propagate request
     */
    public void handleSnapshotRequest(Message requestMessage, SnapshotCollector collector) {
        // Only process one request per snapshot round
        if (processedRequest) {
            return;
        }

        int reqInitiatorId = requestMessage.getOriginalSenderInfo().getId();
        int senderId = requestMessage.getOriginalSenderInfo().getId();

        AppConfig.timestampedStandardPrint("Processing snapshot request from initiator " + reqInitiatorId);

        // Block transactions
        blockSnapshot();
        processedRequest = true;
        initiatorId = reqInitiatorId;

        // Record local state
        int localAmount = getCurrentBitcakeAmount();
        AppConfig.timestampedStandardPrint("CC snapshot - local state recorded: " + localAmount);

        // Create snapshot result and send to initiator
        CCSnapshotResult result = new CCSnapshotResult(AppConfig.myServentInfo.getId(), localAmount);

        // Add our info to collector if we are the initiator
        if (reqInitiatorId == AppConfig.myServentInfo.getId()) {
            ((SnapshotCollectorWorker)collector).addCCSnapshotInfo(
                    AppConfig.myServentInfo.getId(), result);
        } else {
            // Send ACK to initiator with our snapshot data
            CCAckMessage ackMessage = new CCAckMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(reqInitiatorId),
                    String.valueOf(localAmount)
            );
            MessageUtil.sendMessage(ackMessage);
        }

        // Forward request to all neighbors except the sender
        for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
            if (neighborId == senderId) {
                continue; // Skip the node that sent us the request
            }

            ServentInfo neighborInfo = AppConfig.getInfoById(neighborId);
            CCSnapshotRequestMessage forwardRequest = new CCSnapshotRequestMessage(
                    requestMessage.getOriginalSenderInfo(), // Keep original initiator
                    neighborInfo
            );
            AppConfig.timestampedStandardPrint("Forwarding CC_SNAPSHOT_REQUEST to " + neighborId);
            MessageUtil.sendMessage(forwardRequest);
        }
    }

}
