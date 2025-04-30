package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.CCBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.CCAckMessage;
import servent.message.util.MessageUtil;

public class CCSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public CCSnapshotRequestHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CC_SNAPSHOT_REQUEST) {
            AppConfig.timestampedErrorPrint("CCSnapshotRequestHandler got wrong message type: " + clientMessage);
            return;
        }

        AppConfig.timestampedStandardPrint("Received CC SNAPSHOT_REQUEST from " +
                clientMessage.getOriginalSenderInfo().getId());

        if (snapshotCollector.getBitcakeManager() instanceof CCBitcakeManager ccManager) {
            ccManager.handleSnapshotRequest(clientMessage, snapshotCollector);
        }
    }
}
