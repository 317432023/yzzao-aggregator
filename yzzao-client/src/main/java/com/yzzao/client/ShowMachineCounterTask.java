package com.yzzao.client;


public class ShowMachineCounterTask implements Runnable {

	@Override
	public void run() {
		
		for(;;) {
			CountHolder.printAll();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
