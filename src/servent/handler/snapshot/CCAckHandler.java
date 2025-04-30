package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.CCSnapshotResult;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class CCAckHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollectorWorker collectorWorker;

    public CCAckHandler(Message clientMessage, SnapshotCollectorWorker collectorWorker) {
        this.clientMessage = clientMessage;
        this.collectorWorker = collectorWorker;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CC_ACK) {
            AppConfig.timestampedErrorPrint("CCAckHandler got wrong message type: " + clientMessage);
            return;
        }

        int senderId = clientMessage.getOriginalSenderInfo().getId();
        String text = clientMessage.getMessageText();
        int amount = Integer.parseInt(text);

        CCSnapshotResult result = new CCSnapshotResult(senderId, amount);
        collectorWorker.addCCSnapshotInfo(senderId, result);

        AppConfig.timestampedStandardPrint("Received ACK from node " + senderId + " with amount " + amount);
    }
}