package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.ab_acharya_badrinath.ABBitcakeManager;
import app.snapshot_bitcake.av_alagar_venkatesan.AVBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);

			if(bitcakeManager instanceof ABBitcakeManager){
				CausalBroadcastShared.addReceivedTransaction(clientMessage);
				CausalBroadcastShared.commitCausalMessage(clientMessage);

			}

			if (bitcakeManager instanceof AVBitcakeManager) {
				CausalBroadcastShared.addReceivedTransaction(clientMessage);
				CausalBroadcastShared.commitCausalMessage(clientMessage);
				((AVBitcakeManager) bitcakeManager).logIfOld(clientMessage);
			}



		}
		else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
