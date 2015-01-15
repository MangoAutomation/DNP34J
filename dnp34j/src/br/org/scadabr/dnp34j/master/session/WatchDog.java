package br.org.scadabr.dnp34j.master.session;

public class WatchDog extends Thread {
	private DNPUser user;
	private boolean STOP;

	public WatchDog(DNPUser user) {
		this.user = user;
	}

	public void run() {
		System.out.println("[Watchdog] Started!");
		STOP = false;
		while (!STOP) {
			if (!user.getAppRcv().isAlive())
				System.out.print("appRcv is dead!");
			if (!user.getAppSnd().isAlive())
				System.out.print("appSnd is dead!");
			if (!user.getLnkRcv().isAlive())
				System.out.print("lnkRcv is dead!");
			if (!user.getLnkSnd().isAlive())
				System.out.print("lnkSnd is dead!");

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void setSTOP(boolean sTOP) {
		STOP = sTOP;
	}

	public boolean isSTOP() {
		return STOP;
	}
}
