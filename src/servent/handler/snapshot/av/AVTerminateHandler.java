package servent.handler.snapshot.av;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import app.snapshot_bitcake.av_alagar_venkatesan.AVBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class AVTerminateHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollectorWorker snapshotCollector;

    public AVTerminateHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }
    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AV_TERMINATE) {
            AppConfig.timestampedErrorPrint("Invalid message in AVTerminateHandler: " + clientMessage);
            return;
        }

        AppConfig.timestampedStandardPrint("Received AV_TERMINATE. Stopping AV logging...");

        AVBitcakeManager bitcakeManager = (AVBitcakeManager) snapshotCollector.getBitcakeManager();

        // Stop tracking old messages
        bitcakeManager.stopLogging();

        // Print recorded result
        int localAmount = bitcakeManager.getCurrentBitcakeAmount();
        AppConfig.timestampedStandardPrint("AV snapshot result for node " + AppConfig.myServentInfo.getId() +
                ": " + localAmount + " bitcakes");

        Map<Integer, List<Integer>> channelStates = bitcakeManager.getChannelStates();

        for (Map.Entry<Integer, List<Integer>> entry : channelStates.entrySet()) {
            int senderId = entry.getKey();
            List<Integer> oldMessages = entry.getValue();
            int total = oldMessages.stream().mapToInt(Integer::intValue).sum();

            AppConfig.timestampedStandardPrint("Channel from " + senderId + " → " + AppConfig.myServentInfo.getId() +
                    " had " + total + " bitcakes (old messages: " + oldMessages + ")");
        }
    }
}
