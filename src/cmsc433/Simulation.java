package cmsc433; 
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.*; 

/**
 * Simulation is the main class used to run the simulation. You may
 * add any fields (static or instance) or any methods you wish.
 */
public class Simulation {
	// List to track simulation events during simulation
	public static List<SimulationEvent> events;

	/**
	 * Used by other classes in the simulation to log events
	 * 
	 * @param event
	 */
	public static void logEvent(SimulationEvent event) {
		events.add(event);
		System.out.println(event);
	}

	// MY CODE

	private static int numCustomers;
	private static int numCooks;
	private static int numTables;
	private static int machineCapacity;
	private static boolean randomOrders;
	private static int numFinishedOrders;

	public static List<Customer> tables;

	private static HashMap<Food, Machines> machinesMap;

	public static HashMap<Integer, List<Food>> ordersByNum;

	public static HashSet<Integer> newOrders;
	public static LinkedHashSet<Integer> ordersInProgress;
	public static HashSet<Integer> ordersFinished;
	public static HashMap<Integer, Object> locksByNumOrder;

	private static Object lockFinished; 
	// MY CODE

	/**
	 * Function responsible for performing the simulation. Returns a List of
	 * SimulationEvent objects, constructed any way you see fit. This List will
	 * be validated by a call to Validate.validateSimulation. This method is
	 * called from Simulation.main(). We should be able to test your code by
	 * only calling runSimulation.
	 * 
	 * Parameters:
	 * 
	 * @param totalCustomers the number of customers wanting to enter the restaurant
	 * @param totalCooks number of cooks in the simulation
	 * @param totalTables the number of tables in the restaurant
	 * @param totalMachine the count of all machine types in the restaurant
	 * @param randomOrders a flag say whether or not to give each customer a random
	 *        order
	 */
	public static List<SimulationEvent> runSimulation(
		int totalCustomers, int totalCooks,
		int totalTables, int totalMachine,
		boolean randomOrder) {

		// This method's signature MUST NOT CHANGE.

		/*
		 * We are providing this events list object for you.
		 * It is the ONLY PLACE where a concurrent collection object is
		 * allowed to be used.
		 */
		events = Collections.synchronizedList(new ArrayList<SimulationEvent>());



		// Start the simulation
		logEvent(SimulationEvent.startSimulation(totalCustomers,
			totalCooks,
			totalTables,
			totalMachine));

		// Set things up you might need
		numCustomers = totalCustomers; 
		numCooks = totalCooks;
		numTables = totalTables;
		machineCapacity = totalMachine;
		randomOrders = randomOrder;
		numFinishedOrders = 0;

		tables = new ArrayList<Customer>(numTables);
		machinesMap = new HashMap<Food, Machines>(machineCapacity);

		ordersByNum = new HashMap<Integer, List<Food>>();
		locksByNumOrder = new HashMap<Integer, Object>(); 

		newOrders = new HashSet<Integer>();
		ordersInProgress = new LinkedHashSet<Integer>();
		ordersFinished = new HashSet<Integer>();
		lockFinished = new Object(); 

		// Start up machines
		machinesMap.put(FoodType.pizza, new Machines(Machines.MachineType.ovens, FoodType.pizza, machineCapacity)); 
		machinesMap.put(FoodType.subs, new Machines(Machines.MachineType.grillPresses, FoodType.subs, machineCapacity)); 
		machinesMap.put(FoodType.fries, new Machines(Machines.MachineType.fryers, FoodType.fries, machineCapacity)); 
		machinesMap.put(FoodType.soda, new Machines(Machines.MachineType.sodaMachines, FoodType.soda, machineCapacity)); 
		

		// Let cooks in
		Thread[] cookThreads = new Thread[numCooks];

		// MY CODE (build the cooks)
		for(int index = 0; index < cookThreads.length; index++){
			cookThreads[index] = new Thread(new Cook("Cook " + index, machinesMap)); 
		}

		Semaphore sem = new Semaphore(totalTables); 
		// MY CODE

		// Build the customers.
		Thread[] customers = new Thread[numCustomers];
		LinkedList<Food> order;
		if (!randomOrders) { // set number of orders  
			for (int i = 0; i < customers.length; i++) {
				order = new LinkedList<Food>();
				order.add(FoodType.fries);
				order.add(FoodType.pizza);
				order.add(FoodType.subs);
				order.add(FoodType.soda);
				customers[i] = new Thread(new Customer("Customer " + (i), order, sem));
			}
		} else { // random number of orders
			for (int i = 0; i < customers.length; i++) {
				Random rnd = new Random();
				int friesCount = rnd.nextInt(4);
				int pizzaCount = rnd.nextInt(4);
				int subCount = rnd.nextInt(4);
				int sodaCount = rnd.nextInt(4);
				order = new LinkedList<Food>();
				for (int b = 0; b < friesCount; b++) {
					order.add(FoodType.fries);
				}
				for (int f = 0; f < pizzaCount; f++) {
					order.add(FoodType.pizza);
				}
				for (int f = 0; f < subCount; f++) {
					order.add(FoodType.subs);
				}
				for (int c = 0; c < sodaCount; c++) {
					order.add(FoodType.soda);
				}
				customers[i] = new Thread(
					new Customer("Customer " + (i), order, sem));
			}
		}

		// MY CODE
		for (int i = 0; i < cookThreads.length; i++){
			cookThreads[i].start();
		} 

		// MY CODE
		/*
		 * Now "let the customers know the shop is open" by starting them running in
		 * their own thread.
		 */
		for (int i = 0; i < customers.length; i++) {
			customers[i].start();
			/*
			 * NOTE: Starting the customer does NOT mean they get to go right into the shop.
			 * There has to be a table for them. The Customer class' run method has many
			 * jobs to do - one of these is waiting for an available table...
			 */
		}

		try {
			/*
			 * Wait for customers to finish
			 * -- you need to add some code here...
			 */

			for (Thread customer : customers){ // waits for every customer to finish 
				customer.join();
			}

			while (!ordersAllFinished()) {
				synchronized (lockFinished) {
					try {
						lockFinished.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			/*
			 * Then send cooks home...
			 * The easiest way to do this might be the following, where we interrupt their
			 * threads. There are other approaches though, so you can change this if you
			 * want to.
			 */
			for (int i = 0; i < cookThreads.length; i++)
				cookThreads[i].interrupt();
			for (int i = 0; i < cookThreads.length; i++)
				cookThreads[i].join();

		} catch (InterruptedException e) {
			System.out.println("Simulation thread interrupted.");
		}

		// Shut down machines





		// Done with simulation
		logEvent(SimulationEvent.endSimulation());
		return events;
	}

	public static void enterRestaurant(Customer customer){
		synchronized(tables){
			while(tables.size() >= numTables){
				try{
					tables.wait();
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}
			tables.add(customer); 
		}
	}
	public static void exitRestaurant(Customer customer){
		synchronized(tables){
			tables.remove(customer); 
			tables.notifyAll();
		}
	}

	/**
	 *  Places an order. Add order the the hashmap keeping track of the orders and hashmap that 
	 *  has all the locks of each order 
	 * @param customer
	 * @param order
	 * @param orderNum
	 */
	public static void placeOrder(List<Food> order, int orderNum){

		synchronized(ordersByNum){
			ordersByNum.put(orderNum, order); 
		}
		synchronized(locksByNumOrder){
			locksByNumOrder.put(orderNum, new Object()); 
		}
		synchronized(newOrders){
			newOrders.add(orderNum);
			newOrders.notifyAll();
		}
		
	}

	public static HashSet<Integer> getNewOrders(){
		synchronized(newOrders){
			return newOrders; 
		}
	}

	public static boolean hasNewOrders(){
		synchronized(newOrders){
			return !(newOrders.isEmpty()); 
		}
	}

	public static boolean ordersAllFinished(){
		synchronized (lockFinished) {
			return numFinishedOrders == numCustomers;
		}
	}

	public static Integer getNextOrder(){
		synchronized(newOrders){
			while(newOrders.isEmpty() && !(ordersAllFinished())){
				if (ordersAllFinished()) {
					//System.out.println("Returning a null"); 
					return null;
				}
				try {
					newOrders.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (ordersAllFinished()) {
					System.out.println("Returning a null 2"); 
					return null;
				}
			}

			Iterator<Integer> it = newOrders.iterator();
			Integer orderNumber = -1;
			while (it.hasNext()) {
				orderNumber = it.next();
				break;
			}
			newOrders.remove(orderNumber);
			return orderNumber;
		}
	}

	public static List<Food> getOrder(int orderNum){
		synchronized(ordersByNum){
			return ordersByNum.get(orderNum); 
		}
	}

	/**
	 *  Find if the order is still in Progress or if it is finished 
	 * @param orderNum
	 * @return
	 */
	public static boolean isOrderInProgress(int orderNum){
		return ordersInProgress.contains(orderNum); 
	}

	/**
	 *  Order is currently being cooked, add it to list of other orders currently being cooked 
	 * @param orderNum
	 */
	public static void startOrder(int orderNum){
		synchronized(getOrderLock(orderNum)){
			synchronized(ordersInProgress){
				ordersInProgress.add(orderNum); 
				getOrderLock(orderNum).notifyAll();
			}
		}
	}

	public static void finishOrder(Cook cook, int orderNum){
		synchronized (getOrderLock(orderNum)) {
			synchronized (ordersInProgress) {
				ordersInProgress.remove(orderNum);
				synchronized (ordersFinished) {
					ordersFinished.add(orderNum);
				}
				synchronized (lockFinished) {
					numFinishedOrders += 1;
				}
			}
			getOrderLock(orderNum).notifyAll();
		}
	}

	public static boolean isOrderFinished(int orderNum) {
		return ordersFinished.contains(orderNum); 
	}
	public static Object getOrderLock(int orderNumber) {
		synchronized (locksByNumOrder) {
			return locksByNumOrder.get(orderNumber);
		}
	}

	/**
	 * Entry point for the simulation.
	 *
	 * @param args the command-line arguments for the simulation. There
	 *        should be exactly four arguments: the first is the number of
	 *        customers, the second is the number of cooks, the third is the number
	 *        of tables in the restaurant, and the fourth is the number of items
	 *        each set of machines can make at the same time.
	 */
	public static void main(String args[]) throws InterruptedException {
		// Command line argument parser

// @formatter:off
/*
  		if (args.length != 4) {
  			System.err.println("usage: java Simulation <#customers> <#cooks> <#tables> <count> <randomorders");
  			System.exit(1);
  		}
  		int numCustomers = new Integer(args[0]).intValue();
  		int numCooks = new Integer(args[1]).intValue();
  		int numTables = new Integer(args[2]).intValue();
  		int machineCapacity = new Integer(args[3]).intValue();
  		boolean randomOrders = new Boolean(args[4]);
 */
// @formatter: on
		
		// Parameters to the simulation
		int numCustomers = 10;
		int numCooks = 2;
		int numTables = 5;
		int machineCapacity = 4;
		boolean randomOrders = false;

		/* Run the simulation and then feed the result into the method to validate simulation. */
		System.out.println("Did it work? " +
			Validate.validateSimulation(
				runSimulation(
					numCustomers, numCooks,
					numTables, machineCapacity,
					randomOrders)));
	}
	
}
