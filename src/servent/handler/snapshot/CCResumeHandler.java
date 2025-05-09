package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.cc_coordinated_checkpointing.CCBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;

public class CCResumeHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public CCResumeHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }


    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Received CC RESUME. Resuming normal operation...");

        if (snapshotCollector.getBitcakeManager() instanceof CCBitcakeManager ccManager) {
            ccManager.unblockSnapshot();
        }
    }
}
