package cmsc433; 
import java.util.*; 

/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Customer and process them.
 */
public class Cook implements Runnable {
	private final String name;

	private HashMap<Food, Machines> machinesByFood;
	private HashMap<Integer, HashMap<Food, LinkedList<Thread>>> foodThreadsByOrderNum;  
	/**
	 * You can feel free modify this constructor. It must
	 * take at least the name, but may take other parameters
	 * if you would find adding them useful.
	 *
	 * @param: the name of the cook
	 */
	public Cook(String name, HashMap<Food, Machines> machinesByFood) {
		this.name = name;
		this.machinesByFood = machinesByFood; 
		this.foodThreadsByOrderNum = new HashMap<Integer, HashMap<Food, LinkedList<Thread>>>(); 
	}

	public String toString() {
		return name;
	}

	/**
	 * This method executes as follows. The cook tries to retrieve
	 * orders placed by Customers. For each order, a List<Food>, the
	 * cook submits each Food item in the List to an appropriate
	 * Machine type, by calling makeFood(). Once all machines have
	 * produced the desired Food, the order is complete, and the Customer
	 * is notified. The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some
	 * other thread calls the interrupt() method on it, which could
	 * raise InterruptedException if the cook is blocking), then it
	 * terminates.
	 */
	public void run() {

		Simulation.logEvent(SimulationEvent.cookStarting(this));
		try {
			while (true) {
				// YOUR CODE GOES HERE..
				Integer orderNum; 
				synchronized(Simulation.getNewOrders()){
					while(!(Simulation.hasNewOrders())){
						Simulation.getNewOrders().wait();
					}
					orderNum =  Simulation.getNextOrder(); 
				}

				List<Food> order = Simulation.getOrder(orderNum); 

				processOrder(order, orderNum); 
			}
		} catch (InterruptedException e) {
			// This code assumes the provided code in the Simulation class
			// that interrupts each cook thread when all customers are done.
			// You might need to change this if you change how things are
			// done in the Simulation class.
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}

	public void processOrder(List<Food> order, int orderNum) throws InterruptedException{
		synchronized(Simulation.getOrderLock(orderNum)){
			Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, order, orderNum));
			foodThreadsByOrderNum.put(orderNum, new HashMap<Food, LinkedList<Thread>>());
			Food[] foods = { FoodType.fries, FoodType.pizza, FoodType.subs, FoodType.soda };
			int[] foodCounts = new int[4];
			for(Food food : order){
				if (food == FoodType.fries){
					foodCounts[0] += 1; 
				} else if (food == FoodType.pizza){
					foodCounts[1] += 1; 
				} else if (food == FoodType.subs){
					foodCounts[2] += 1; 
				} else if (food == FoodType.soda){
					foodCounts[3] += 1; 
				} else {
					throw new UnsupportedOperationException("Unknown food type was entered: " + food.name); 
				}
			}

			for (int i = 0; i < 4; i++){
				Food food = foods[i]; 
				foodThreadsByOrderNum.get(orderNum).put(food, new LinkedList<Thread>());
				for(int j = 0; j < foodCounts[i]; j++){
					Machines machine = machinesByFood.get(food);
					Simulation.logEvent(SimulationEvent.cookStartedFood(this, food, orderNum));
					Simulation.startOrder(orderNum);
					Thread thread = machine.makeFood(food);
					foodThreadsByOrderNum.get(orderNum).get(food).add(thread);
				}
			}

			for (Food food : foodThreadsByOrderNum.get(orderNum).keySet()) {
				for (Thread thread : foodThreadsByOrderNum.get(orderNum).get(food)) {
					thread.join();
					Simulation.logEvent(SimulationEvent.cookFinishedFood(this, food, orderNum));
				}
			}
			
			Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, orderNum));
			Simulation.finishOrder(this, orderNum); 
			
		}
	}
}
