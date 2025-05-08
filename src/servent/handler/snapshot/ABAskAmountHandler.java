package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ABTellAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ABAskAmountHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public ABAskAmountHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ACHARYA_BADRINATH_ASK_AMOUNT) {
            BitcakeManager bitcakeManager = snapshotCollector.getBitcakeManager();
            int currentAmount = bitcakeManager.getCurrentBitcakeAmount();

            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

            Message tellMessage = new ABTellAmountMessage(
                    AppConfig.myServentInfo, clientMessage.getOriginalSenderInfo(),
                    null, vectorClock, currentAmount,
                    CausalBroadcastShared.getSendTransactions(),
                    CausalBroadcastShared.getReceivedTransactions()
            );

            CausalBroadcastShared.commitCausalMessage(tellMessage);
            MessageUtil.sendMessage(tellMessage);


            AppConfig.timestampedStandardPrint("Sent AB tell response to " + clientMessage.getOriginalSenderInfo().getId() +
                    " with amount: " + currentAmount);

        } else {
            AppConfig.timestampedErrorPrint("Ask amount handler got: " + clientMessage);
        }
    }
}