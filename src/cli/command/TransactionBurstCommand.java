package cli.command;

import java.util.concurrent.ThreadLocalRandom;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.ab_acharya_badrinath.ABBitcakeManager;
import app.snapshot_bitcake.av_alagar_venkatesan.AVBitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 10;
	private static final int MAX_TRANSFER_AMOUNT = 10;

	
	private BitcakeManager bitcakeManager;
	
	public TransactionBurstCommand(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);
					
					int amount = 1 + rand.nextInt(MAX_TRANSFER_AMOUNT);
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */


//					CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());

					Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo, neighborInfo, amount, bitcakeManager);

					if(bitcakeManager instanceof ABBitcakeManager || bitcakeManager instanceof AVBitcakeManager) {
						CausalBroadcastShared.addSendTransaction(transactionMessage);
					}


					MessageUtil.sendMessage(transactionMessage);
				}
				
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
		}
	}

	
}
