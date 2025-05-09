package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ABAckMessage;

public class ABAckHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public ABAckHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.ACHARYA_BADRINATH_TELL_AMOUNT) {
                int neighborAmount = Integer.parseInt(clientMessage.getMessageText());
                ABAckMessage tellAmountMessage = (ABAckMessage) clientMessage;

                snapshotCollector.addAcharyaBadrinathSnapshotInfo(
                        "node" + clientMessage.getOriginalSenderInfo().getId(),
                        neighborAmount,
                        tellAmountMessage.getSendTransactions(),
                        tellAmountMessage.getReceivedTransactions()
                );
            } else {
                AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint(e.getMessage());
        }
    }
}