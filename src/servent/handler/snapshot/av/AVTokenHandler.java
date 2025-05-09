package servent.handler.snapshot.av;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import app.snapshot_bitcake.av_alagar_venkatesan.AVBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.av.AVDoneMessage;
import servent.message.snapshot.av.AVTokenMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class AVTokenHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;


    public AVTokenHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AV_TOKEN) {
            AppConfig.timestampedErrorPrint("Invalid message type in AVTokenHandler: " + clientMessage);
            return;
        }

        AppConfig.timestampedStandardPrint("Processing AV snapshot token...");

        AVTokenMessage tokenMessage = (AVTokenMessage) clientMessage;

        // Record bitcake amount
        AVBitcakeManager bitcakeManager = (AVBitcakeManager)snapshotCollector.getBitcakeManager();
        int localAmount = bitcakeManager.getCurrentBitcakeAmount();

        AppConfig.timestampedStandardPrint("Recorded local bitcake amount: " + localAmount);

        // Start logging old messages
        Map<Integer, Integer> tokenClock = tokenMessage.getSenderVectorClock();
        bitcakeManager.startLogging(tokenClock);

        // Save snapshot result
        snapshotCollector.addAVSnapshotInfo(AppConfig.myServentInfo.getId(), localAmount);

        // Send AV_DONE to initiator
        ServentInfo initiator = tokenMessage.getOriginalSenderInfo();
        AVDoneMessage doneMessage = new AVDoneMessage(MessageType.AV_DONE, AppConfig.myServentInfo, initiator, tokenClock);
        MessageUtil.sendMessage(doneMessage);

        AppConfig.timestampedStandardPrint("Sent AV_DONE to initiator " + initiator.getId());
    }
}
