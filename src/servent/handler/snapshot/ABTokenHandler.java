package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ABAckMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ABTokenHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public ABTokenHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ACHARYA_BADRINATH_ASK_AMOUNT) {
            BitcakeManager bitcakeManager = snapshotCollector.getBitcakeManager();

            //02. Process pᵢ records its local state upon receiving the token.
            int currentAmount = bitcakeManager.getCurrentBitcakeAmount();

            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());


            //03. Each process maintains SENT and RECD arrays, and sends them to the initiator.
            Message tellMessage = new ABAckMessage(
                    AppConfig.myServentInfo, clientMessage.getOriginalSenderInfo(),
                    null, vectorClock, currentAmount,
                    CausalBroadcastShared.getSendTransactions(),
                    CausalBroadcastShared.getReceivedTransactions()
            );

            CausalBroadcastShared.commitCausalMessage(tellMessage);
            //04. Process pᵢ sends an acknowledgment to the initiator.
            MessageUtil.sendMessage(tellMessage);


            AppConfig.timestampedStandardPrint("Sent AB tell response to " + clientMessage.getOriginalSenderInfo().getId() +
                    " with amount: " + currentAmount);

        } else {
            AppConfig.timestampedErrorPrint("Ask amount handler got: " + clientMessage);
        }
    }
}