package cmsc433; 


import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Customers are simulation actors that have two fields: a name, and a list
 * of Food items that constitute the Customer's order. When running, an
 * customer attempts to enter the Ratsie's (only successful if the
 * Ratsie's has a free table), place its order, and then leave the
 * Ratsie's when the order is complete.
 */
public class Customer implements Runnable {
	// JUST ONE SET OF IDEAS ON HOW TO SET THINGS UP...
	private final String name;
	private final List<Food> order;
	private final int orderNum;
	private Semaphore sem; 
	private static int runningCounter = 0;
	private static Object runningCounterLock = new Object(); 

	/**
	 * You can feel free modify this constructor. It must take at
	 * least the name and order but may take other parameters if you
	 * would find adding them useful.
	 */
	public Customer(String name, List<Food> order, Semaphore sema) {
		this.name = name;
		this.order = order;
		this.sem = sema; // semaphore to set the limit on the total threads running concurrently 
		synchronized(runningCounterLock) {
			this.orderNum = ++runningCounter;	
		}
		
	}

	public String toString() {
		return name;
	}

	/**
	 * This method defines what an Customer does: The customer attempts to
	 * enter the Ratsie's (only successful when the Ratsie's has a
	 * free table), place its order, and then leave the Ratsie's
	 * when the order is complete.
	 */
	public void run() {
		// YOUR CODE GOES HERE...
		Simulation.logEvent(SimulationEvent.customerStarting(this));

		// **** check if there are tables open **** 
		try{
			sem.acquire();
		} catch(InterruptedException e){
			e.printStackTrace();
		}
		// enter Ratsie's
		Simulation.enterRestaurant(this); 
		Simulation.logEvent(SimulationEvent.customerEnteredRatsies(this));

		
		// place an order
		Simulation.logEvent(SimulationEvent.customerPlacedOrder(this, order, orderNum)); 
		Simulation.placeOrder(order, orderNum); 

		// wait for order
		
		synchronized(Simulation.getOrderLock(orderNum)){
			while(!Simulation.isOrderFinished(orderNum)){
				try{
					Simulation.getOrderLock(orderNum).wait();
				} catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}

		// eat the order
		Simulation.logEvent(SimulationEvent.customerReceivedOrder(this, order, orderNum));

		// leave 
		Simulation.logEvent(SimulationEvent.customerLeavingRatsies(this));
		Simulation.exitRestaurant(this);
		sem.release();
	}
}
